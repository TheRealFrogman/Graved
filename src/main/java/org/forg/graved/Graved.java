package org.forg.graved;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.UUID;

class GravedChest {
    private static final NamespacedKey GRAVED_CHEST_META_KEY
            = new NamespacedKey(Graved.getInstance(), "GRAVED_CHEST");
    private static final NamespacedKey CHEST_DISPLAY_UUID
            = new NamespacedKey(Graved.getInstance(), "CHEST_DISPLAY");

    public final Chest c;
    public final Player p;
    private GravedChest(Chest c, Player p) {
        this.c = c;
        this.p = p;
    }
    public static GravedChest createAndRegister(Chest c, Player p, List<ItemStack> contents, String toptext,String bottomtext){
        GravedChest gc = new GravedChest(c, p);

        PersistentDataContainer chestMetaContainer = gc.c.getPersistentDataContainer();

        TextDisplay display = makeGravedChestDisplay(c,p,toptext,bottomtext);

        chestMetaContainer.set(CHEST_DISPLAY_UUID, PersistentDataType.STRING, display.getUniqueId().toString());
        chestMetaContainer.set(GRAVED_CHEST_META_KEY, PersistentDataType.BOOLEAN,true);

        gc.c.update(); // required fsr

        Inventory chest_inventory = gc.c.getBlockInventory();
        for (ItemStack item : contents) {
            if(item != null) {
                chest_inventory.addItem(item);
            }
        }


        return gc;
    }
    @Nonnull
    private static TextDisplay makeGravedChestDisplay(Chest c,Player p, String toptext, String bottomtext) {
        Location cl = c.getLocation();
        World cw = c.getWorld();

        cl.setY(cl.getY() + 2);
        cl.setX(cl.getX() + 0.5);
        cl.setZ(cl.getZ() + 0.5);

        TextDisplay text_display = cw.spawn(cl, TextDisplay.class);
        text_display.setBillboard(Display.Billboard.CENTER);

        text_display.setText(
                (toptext != null ? toptext + "\n\n" : "")
                        + p.getName()
                        + (bottomtext != null ? "\n\n" + bottomtext : "")
        );
        return text_display;
    }

    @Nullable
    public static GravedChest GetIfGraved(Chest c, Player p) {
        PersistentDataContainer chestPersistentMeta = c.getPersistentDataContainer();
        if (
            chestPersistentMeta.has(GRAVED_CHEST_META_KEY)
            && chestPersistentMeta.has(CHEST_DISPLAY_UUID)
        ) {
            return new GravedChest(c,p);
        }
        return null;
    }
    void remove() {
        PersistentDataContainer chestPersistentMeta = this.c.getPersistentDataContainer();
        this.c.getBlock().setType(Material.AIR);

        if (!chestPersistentMeta.has(GRAVED_CHEST_META_KEY)) return;
        String maybeUUID = chestPersistentMeta.get(CHEST_DISPLAY_UUID, PersistentDataType.STRING);
        if(maybeUUID == null) return;
        UUID displayUUID = UUID.fromString(maybeUUID);

        Entity display = Graved.getInstance().getServer().getEntity(displayUUID);
        if(display == null) return;
        display.remove();
    }

    boolean isEmpty(){
        return c.getInventory().isEmpty();
    }
}

public class Graved extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("Starting");
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(this, this);

        getLogger().info("Plugin has been enabled");

    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("Aboba plugin has ben offed");
    }
    static JavaPlugin getInstance() {
        return getPlugin(Graved.class);
    }
    @EventHandler
    void onDeath(PlayerDeathEvent e) {
        List<ItemStack> drops = e.getDrops();
        if(drops.isEmpty()) return;

        Player p = e.getEntity();

        Location location = p.getLocation();

        // defining the block to change
        Block b = location.getBlock();

        b.setType(Material.CHEST);
        BlockState blockState = b.getState();
        Chest chest = (Chest)blockState;

        GravedChest.createAndRegister(chest, p,drops,"Grave of player","R.I.P.");
        drops.clear();
    }

    @EventHandler
    void onChestClosed(InventoryCloseEvent e) {
        Inventory i = e.getInventory();
        InventoryHolder ih = i.getHolder();

        // e.getPlayer returns HumanEntity not player ???
        HumanEntity he = e.getPlayer();
        Player p = getServer().getPlayer(he.getName());
        //

        if(!(ih instanceof  Chest)) return;
        Chest c = (Chest)ih;
        GravedChest gc = GravedChest.GetIfGraved(c, p);
        if(gc == null) return;

        if (gc.isEmpty()) gc.remove();
    }
    @EventHandler
    void onGravedChestBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        BlockState bs = b.getState();

        if(!(bs instanceof  Chest)) return;
        Chest c = (Chest)bs;

        GravedChest gc = GravedChest.GetIfGraved(c, e.getPlayer());
        if(gc == null) return;

        gc.remove();
    }
}

package org.forg.graved;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;

class GravedChest {
    private static final HashMap<GravedChest, TextDisplay> chest_to_text = new HashMap<GravedChest, TextDisplay>();
    private static final NamespacedKey GRAVED_META_KEY
            = new NamespacedKey(Graved.getInstance(), "GRAVED_CHEST");

    public final Chest c;
    public final Player p;
    public final String bottomtext;
    private GravedChest(Chest c, Player p, String bottomtext) {
        this.c = c;
        this.p = p;
        this.bottomtext = bottomtext;
    }
    public static GravedChest createAndRegister(Chest c, Player p, List<ItemStack> contents, String bottomtext){
        GravedChest gc = new GravedChest(c, p, bottomtext);
        gc.c.getPersistentDataContainer().set(GRAVED_META_KEY, PersistentDataType.BOOLEAN,true);
        gc.c.update();

        Inventory chest_inventory = c.getBlockInventory();
        for(ItemStack item : contents){
            if(item != null) {
                chest_inventory.addItem(item);
            }
        }
        chest_to_text.put(gc, makeGravedChestDisplay(c,p,bottomtext));
        return gc;
    }
    @Nonnull
    private static TextDisplay makeGravedChestDisplay(Chest c,Player p, String bottomtext) {
        Location cl = c.getLocation();
        World cw = c.getWorld();

        cl.setY(cl.getY() + 2);
        cl.setX(cl.getX() + 0.5);
        cl.setZ(cl.getZ() + 0.5);

        TextDisplay text_display = cw.spawn(cl, TextDisplay.class);
        text_display.setBillboard(Display.Billboard.CENTER);

        text_display.setText(p.getName() + (bottomtext != null ? "\n\n" + bottomtext : ""));
        return text_display;
    }
    @Nullable
    public static GravedChest GetIfGraved(Chest c) {

        if (c.getPersistentDataContainer().has(GRAVED_META_KEY, PersistentDataType.BOOLEAN)) {
            // Find the GravedChest instance associated with this Chest
            for (GravedChest gc : chest_to_text.keySet()) {
                if (gc.c.getBlock().getState().equals(c.getBlock().getState())) {
                    return gc;
                }
            }
        }
        return null;
    }
    void remove() {
        this.c.getBlock().setType(Material.AIR);
        chest_to_text.get(this).remove();
        chest_to_text.remove(this);
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

        // здесь наверное нужно будет в сфере 3 на 3 искать рандомный блок куда можно поставить сундукы
        Block b = location.getBlock();

        b.setType(Material.CHEST);
        BlockState blockState = b.getState();
        Chest chest = (Chest)blockState;

        GravedChest.createAndRegister(chest, p,drops,"Sample bottom text");
        drops.clear();
    }

    @EventHandler
    void onChestClosed(InventoryCloseEvent e) {
        Inventory i = e.getInventory();
        InventoryHolder ih = i.getHolder();
        if(!(ih instanceof  Chest)) return;
        Chest c = (Chest)ih;

        GravedChest gc = GravedChest.GetIfGraved(c);
        if(gc == null) return;

        if (gc.isEmpty()) gc.remove();
    }
    @EventHandler
    void onGravedChestBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        BlockState bs = b.getState();

        if(!(bs instanceof  Chest)) return;
        Chest c = (Chest)bs;

        GravedChest gc = GravedChest.GetIfGraved(c);
        if(gc == null) return;

        gc.remove();
    }
}

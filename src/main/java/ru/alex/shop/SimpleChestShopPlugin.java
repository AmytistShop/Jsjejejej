package ru.alex.shop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class SimpleChestShopPlugin extends JavaPlugin implements Listener {

    private ShopManager shopManager;
    private Messages m;

    private Material currency;
    private String currencyName;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();

        shopManager = new ShopManager(getDataFolder());
        shopManager.load();

        getServer().getPluginManager().registerEvents(this, this);
    }

    private void reloadLocal() {
        reloadConfig();
        m = new Messages(getConfig());

        String mat = getConfig().getString("currency.material","DIAMOND").toUpperCase(Locale.ROOT);
        currency = Material.matchMaterial(mat);
        if (currency == null) currency = Material.DIAMOND;
        currencyName = getConfig().getString("currency.name","Алмазов");
    }

    @Override
    public void onDisable() {
        if (shopManager != null) shopManager.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!cmd.getName().equalsIgnoreCase("shop")) return false;

        if (args.length < 1 || !args[0].equalsIgnoreCase("set")) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.need_price_amount","")));
            return true;
        }
        if (args.length < 3) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.need_price_amount","")));
            return true;
        }

        int price, amount;
        try {
            price = Integer.parseInt(args[1]);
            amount = Integer.parseInt(args[2]);
        } catch (Exception ex) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.need_price_amount","")));
            return true;
        }
        if (price <= 0 || amount <= 0) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.need_price_amount","")));
            return true;
        }

        Block target = p.getTargetBlockExact(6);
        if (target == null || !(target.getState() instanceof Container cont)) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.need_look_chest","")));
            return true;
        }

        ItemStack sample = ShopUtil.firstSellable(cont.getInventory(), currency);
        if (sample == null) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.need_item_in_chest","")));
            return true;
        }

        Shop existed = shopManager.getByChest(cont.getLocation());
        boolean isUpdate = existed != null;

        Shop shop = new Shop(cont.getLocation(), p.getUniqueId(), price, amount, sample);

        // place sign on the front side for beauty
        Block signBlock = ShopUtil.placeFrontSign(target);
        if (signBlock != null && signBlock.getState() instanceof Sign sign) {
            ShopUtil.writeSign(sign, m, getConfig(), p.getName(), ShopUtil.prettyName(sample), amount, price, currencyName);
            shop.setSignLoc(sign.getLocation());
        }

        shopManager.put(shop);
        shopManager.save();

        String msg = m.color(getConfig().getString("messages.prefix","") +
                (isUpdate ? getConfig().getString("messages.updated","") : getConfig().getString("messages.created","")));
        msg = msg.replace("%price%", String.valueOf(price))
                 .replace("%amount%", String.valueOf(amount))
                 .replace("%currency%", currencyName);
        p.sendMessage(msg);
        return true;
    }

    // Buyers purchase through chest UI: clicking product in the chest triggers purchase.
    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Container cont)) return;

        Shop shop = shopManager.getByChest(cont.getLocation());
        if (shop == null) return;

        // owner/admin can manage chest freely
        if (shop.getOwner().equals(p.getUniqueId()) || p.hasPermission("simplechestshop.admin")) return;

        // only protect top inventory (the chest)
        if (e.getClickedInventory() == null || e.getClickedInventory() != top) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            e.setCancelled(true);
            return;
        }

        ItemStack sample = shop.getItemSample();

        // block taking currency or any other items; only "buy" by clicking the product
        if (!clicked.isSimilar(sample)) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        int need = shop.getAmount();
        int available = ShopUtil.countSimilar(top, sample);
        if (available < need) {
            p.closeInventory();
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.out_of_stock","")));
            return;
        }

        int price = shop.getPrice();
        int has = ShopUtil.countMaterial(p.getInventory(), currency);
        if (has < price) {
            p.closeInventory();
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") +
                    getConfig().getString("messages.not_enough_currency_close","").replace("%currency%", currencyName)));
            return;
        }

        // take money
        ShopUtil.removeMaterial(p.getInventory(), currency, price);

        // remove items from chest
        ShopUtil.removeSimilar(top, sample, need);

        // give items
        ItemStack give = sample.clone();
        give.setAmount(need);
        ShopUtil.addOrDrop(p.getInventory(), p.getLocation(), give);

        // store money in chest (or drop)
        boolean stored = ShopUtil.addOrDrop(top, cont.getLocation(), new ItemStack(currency, price));
        if (!stored) {
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.chest_full_drop","")));
        }

        String bought = getConfig().getString("messages.bought","");
        bought = bought.replace("%item%", ShopUtil.prettyName(sample))
                       .replace("%amount%", String.valueOf(need))
                       .replace("%price%", String.valueOf(price))
                       .replace("%currency%", currencyName);
        p.sendMessage(m.color(getConfig().getString("messages.prefix","") + bought));
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Container cont)) return;

        Shop shop = shopManager.getByChest(cont.getLocation());
        if (shop == null) return;

        if (shop.getOwner().equals(p.getUniqueId()) || p.hasPermission("simplechestshop.admin")) return;

        int topSize = top.getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // Decorative sign protection: only owner can break (admin optional)
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!(b.getState() instanceof Sign)) return;

        Shop shop = shopManager.getBySign(b.getLocation());
        if (shop == null) return;

        Player p = e.getPlayer();
        if (shop.getOwner().equals(p.getUniqueId()) || p.hasPermission("simplechestshop.admin")) {
            shopManager.removeByChest(shop.getChestLoc());
            shopManager.save();
            p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.removed","")));
            return;
        }

        e.setCancelled(true);
        p.sendMessage(m.color(getConfig().getString("messages.prefix","") + getConfig().getString("messages.break_denied","")));
    }
}

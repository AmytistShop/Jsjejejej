package ru.alex.shop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Map;

public class ShopUtil {

    public static ItemStack firstSellable(Inventory inv, Material currency) {
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            if (it.getType() == currency) continue;
            ItemStack sample = it.clone();
            sample.setAmount(1);
            return sample;
        }
        return null;
    }

    public static String prettyName(ItemStack item) {
        if (item == null) return "";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return strip(meta.getDisplayName());
        return niceEnum(item.getType().name());
    }

    private static String niceEnum(String name) {
        String[] parts = name.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("§.", "").trim();
    }

    public static int countSimilar(Inventory inv, ItemStack sample) {
        int c = 0;
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            if (it.isSimilar(sample)) c += it.getAmount();
        }
        return c;
    }

    public static boolean removeSimilar(Inventory inv, ItemStack sample, int amount) {
        int remaining = amount;
        ItemStack[] contents = inv.getContents();
        for (int i=0;i<contents.length;i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() == Material.AIR) continue;
            if (!it.isSimilar(sample)) continue;

            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            remaining -= take;

            if (it.getAmount() <= 0) contents[i] = null;
            if (remaining <= 0) break;
        }
        inv.setContents(contents);
        return remaining <= 0;
    }

    public static int countMaterial(Inventory inv, Material mat) {
        int c = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == mat) c += it.getAmount();
        }
        return c;
    }

    public static boolean removeMaterial(Inventory inv, Material mat, int amount) {
        return removeSimilar(inv, new ItemStack(mat), amount);
    }

    public static boolean addOrDrop(Inventory inv, Location dropLoc, ItemStack stack) {
        Map<Integer, ItemStack> left = inv.addItem(stack);
        if (left.isEmpty()) return true;
        for (ItemStack it : left.values()) {
            dropLoc.getWorld().dropItemNaturally(dropLoc, it);
        }
        return false;
    }

    public static Container getContainerAt(Location loc) {
        Block b = loc.getBlock();
        BlockState st = b.getState();
        if (st instanceof Container c) return c;
        return null;
    }

    public static Block placeFrontSign(Block chestBlock) {
        if (!(chestBlock.getBlockData() instanceof Chest chestData)) return null;

        var front = chestData.getFacing();
        Block signBlock = chestBlock.getRelative(front);
        if (!signBlock.getType().isAir()) return null;

        signBlock.setType(Material.OAK_WALL_SIGN, false);
        if (!(signBlock.getBlockData() instanceof WallSign ws)) return signBlock;

        ws.setFacing(front);
        signBlock.setBlockData(ws, false);
        return signBlock;
    }

    public static void writeSign(Sign sign, Messages m, org.bukkit.configuration.file.FileConfiguration cfg,
                                 String owner, String item, int amount, int price, String currencyName) {
        String l1 = m.color(cfg.getString("sign.line1","&a[Магазин] &f%owner%"));
        String l2 = m.color(cfg.getString("sign.line2","&e%item% x%amount%"));
        String l3 = m.color(cfg.getString("sign.line3","&6Цена: &e%price% %currency%"));
        String l4 = m.color(cfg.getString("sign.line4",""));

        l1 = replaceAll(l1, owner, item, amount, price, currencyName);
        l2 = replaceAll(l2, owner, item, amount, price, currencyName);
        l3 = replaceAll(l3, owner, item, amount, price, currencyName);
        l4 = replaceAll(l4, owner, item, amount, price, currencyName);

        sign.setLine(0, l1);
        sign.setLine(1, l2);
        sign.setLine(2, l3);
        sign.setLine(3, l4);
        sign.update();
    }

    private static String replaceAll(String s, String owner, String item, int amount, int price, String currency) {
        return s.replace("%owner%", owner)
                .replace("%item%", item)
                .replace("%amount%", String.valueOf(amount))
                .replace("%price%", String.valueOf(price))
                .replace("%currency%", currency);
    }
}

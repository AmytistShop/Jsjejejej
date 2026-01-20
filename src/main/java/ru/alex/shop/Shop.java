package ru.alex.shop;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Shop {
    private final Location chestLoc;
    private Location signLoc;
    private final UUID owner;
    private int price;
    private int amount;
    private ItemStack itemSample;

    public Shop(Location chestLoc, UUID owner, int price, int amount, ItemStack itemSample) {
        this.chestLoc = chestLoc;
        this.owner = owner;
        this.price = price;
        this.amount = amount;
        this.itemSample = itemSample;
    }

    public Location getChestLoc() { return chestLoc; }
    public UUID getOwner() { return owner; }
    public int getPrice() { return price; }
    public int getAmount() { return amount; }
    public ItemStack getItemSample() { return itemSample.clone(); }

    public void setPrice(int price) { this.price = price; }
    public void setAmount(int amount) { this.amount = amount; }
    public void setItemSample(ItemStack itemSample) { this.itemSample = itemSample; }

    public Location getSignLoc() { return signLoc; }
    public void setSignLoc(Location signLoc) { this.signLoc = signLoc; }
}

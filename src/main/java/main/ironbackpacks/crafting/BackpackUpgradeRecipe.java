package main.ironbackpacks.crafting;

import com.sun.istack.internal.Nullable;
import main.ironbackpacks.items.backpacks.ItemBaseBackpack;
import main.ironbackpacks.items.upgrades.ItemUpgradeBase;
import main.ironbackpacks.items.upgrades.UpgradeTypes;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BackpackUpgradeRecipe implements IRecipe {

    /** Is the ItemStack that you get when craft the recipe. */
    private final ItemStack recipeOutput;
    /** Is a List of ItemStack that composes the recipe. */
    public final List recipeItems;

    public BackpackUpgradeRecipe(ItemStack recipeOutput, List recipeItems){
        this.recipeItems = recipeItems;
        this.recipeOutput = recipeOutput;
    }

    public BackpackUpgradeRecipe(ItemStack recipeOutput, Object... items){
        this(recipeOutput, getItemStacks(items));
    }

    public static List<Object> getItemStacks(Object[] items)
    {
        List<Object> recipeItems = new ArrayList<Object>();
        for (Object in : items)
        {
            if (in instanceof ItemStack)
                recipeItems.add(((ItemStack) in).copy());
            else if (in instanceof Item)
                recipeItems.add(new ItemStack((Item) in));
            else if (in instanceof Block)
                recipeItems.add(new ItemStack((Block) in));
            else if (in instanceof String)
                recipeItems.add(OreDictionary.getOres((String) in));
            else
            {
                String ret = "Invalid shapeless ore recipe: ";
                for (Object tmp : items)
                    ret += tmp + ", ";
                throw new RuntimeException(ret);
            }
        }
        return recipeItems;
    }

    public static ItemStack getFirstBackpack(InventoryCrafting inventoryCrafting)
    {
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                ItemStack itemstack = inventoryCrafting.getStackInRowAndColumn(j, i);

                if (itemstack != null && (itemstack.getItem() instanceof ItemBaseBackpack))
                    return itemstack;
            }
        }

        return null;
    }

    public static ItemStack getFirstUpgrade(InventoryCrafting inventoryCrafting){
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                ItemStack itemstack = inventoryCrafting.getStackInRowAndColumn(j, i);

                if (itemstack != null && (itemstack.getItem() instanceof ItemUpgradeBase))
                    return itemstack;
            }
        }

        return null;
    }


    @Override
    public boolean matches(InventoryCrafting inventoryCrafting, World world)
    {
        ArrayList<Object> required = new ArrayList<Object>(this.recipeItems);

        ItemStack backpack = getFirstBackpack(inventoryCrafting);
        if (backpack == null)
            return false;

        for (int x = 0; x < inventoryCrafting.getSizeInventory(); x++)
        {
            ItemStack slot = inventoryCrafting.getStackInSlot(x);

            if (slot != null)
            {
                boolean inRecipe = false;
                Iterator<Object> req = required.iterator();

                while (req.hasNext())
                {
                    boolean match = false;

                    Object next = req.next();

                    if (next instanceof ItemStack)
                    {
                        match = OreDictionary.itemMatches((ItemStack) next, slot, false);
                    }
                    else if (next instanceof ArrayList)
                    {
                        Iterator<ItemStack> itr = ((ArrayList<ItemStack>) next).iterator();
                        while (itr.hasNext() && !match)
                        {
                            match = OreDictionary.itemMatches(itr.next(), slot, false);
                        }
                    }

                    if (match)
                    {
                        inRecipe = true;
                        required.remove(next);
                        break;
                    }
                }

                if (!inRecipe)
                {
                    return false;
                }
            }
        }

        return required.isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {\

        ItemStack backpack = getFirstBackpack(inventoryCrafting);
        ItemStack result = backpack.copy();
        int[] upgrades = ((ItemBaseBackpack) result.getItem()).getUpgradesFromNBT(result);
        NBTTagCompound nbtTagCompound = result.getTagCompound();
        boolean upgradeFound = false;
//        NBTTagByteArray tagArray = new NBTTagByteArray(new byte[3]);
        NBTTagList tagList = new NBTTagList();
//        NBTTagList subList = new NBTTagList();
//        nbtTagCompound.setTag("Upgrades", new NBTTagList()); //this fixes crash
        int counter = -1;
        for (int integer : upgrades){
            counter++;
            System.out.println("Counter: " + counter);
            if (integer == 0 && counter <= ((ItemBaseBackpack) result.getItem()).getUpgradeSlots()){ //If an empty upgrade slot and backpack can support upgrade
                if (!upgradeFound){
                    ItemStack upgrade = getFirstUpgrade(inventoryCrafting);
                    ItemUpgradeBase upgradeBase = (ItemUpgradeBase) upgrade.getItem();
                    NBTTagCompound tagCompound = new NBTTagCompound();
                    tagCompound.setByte("Upgrade", (byte) upgradeBase.getTypeID());
                    tagList.appendTag(tagCompound);
                    upgradeFound = true;

                    System.out.println("Setting upgrade slot: "+counter+" to "+UpgradeTypes.values()[upgradeBase.getTypeID()].getFancyName()); //TODO - not saving NBT upgrade data right?
                }else{
                    NBTTagCompound tagCompound = new NBTTagCompound();
                    tagCompound.setByte("Upgrade", (byte) integer);
                    tagList.appendTag(tagCompound);

                    System.out.println("Setting upgrade slot: "+counter+" to default: "+integer);
                }
            }
        }
        nbtTagCompound.setTag("Upgrades", tagList); //TODO -crash here w/ null pointer
//        nbtTagCompound.setIntArray("Upgrades", tagArray);
        return result;
    }

//    @Override
//    public ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
//        return this.recipeOutput.copy();
//    }

    @Override
    public int getRecipeSize() {
        return this.recipeItems.size();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return recipeOutput;
    }
}

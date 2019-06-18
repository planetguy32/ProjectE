package moze_intel.projecte.gameObjs.items.rings;

import moze_intel.projecte.PECore;
import moze_intel.projecte.api.PESounds;
import moze_intel.projecte.api.item.IPedestalItem;
import moze_intel.projecte.api.item.IProjectileShooter;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntitySWRGProjectile;
import moze_intel.projecte.gameObjs.items.IFlightProvider;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.handlers.InternalAbilities;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SWRG extends ItemPE implements IPedestalItem, IFlightProvider, IProjectileShooter
{
	public SWRG(Properties props)
	{
		super(props);
		addPropertyOverride(new ResourceLocation(PECore.MODID, "mode"), MODE_GETTER);
	}

	private void tick(ItemStack stack, PlayerEntity player)
	{
        if (stack.getOrCreateTag().getInt(TAG_MODE) > 1)
		{
			// Repel on both sides - smooth animation
			WorldHelper.repelEntitiesInAABBFromPoint(player.getEntityWorld(), player.getBoundingBox().grow(5), player.posX, player.posY, player.posZ, true);
		}

		if (player.getEntityWorld().isRemote)
		{
			return;
		}

		ServerPlayerEntity playerMP = (ServerPlayerEntity) player;

		if (getEmc(stack) == 0 && !consumeFuel(player, stack, 64, false))
		{
			if (stack.getTag().getInt(TAG_MODE) > 0)
			{
				changeMode(player, stack, 0);
			}

			if (playerMP.abilities.allowFlying)
			{
				playerMP.getCapability(InternalAbilities.CAPABILITY).ifPresent(InternalAbilities::disableSwrgFlightOverride);
			}

			return;
		}

		if (!playerMP.abilities.allowFlying)
		{
			playerMP.getCapability(InternalAbilities.CAPABILITY).ifPresent(InternalAbilities::enableSwrgFlightOverride);
		}

		if (playerMP.abilities.isFlying)
		{
			if (!isFlyingEnabled(stack))
			{
				changeMode(player, stack, stack.getTag().getInt(TAG_MODE) == 0 ? 1 : 3);
			}
		}
		else
		{
			if (isFlyingEnabled(stack))
			{
				changeMode(player, stack, stack.getTag().getInt(TAG_MODE) == 1 ? 0 : 2);
			}
		}

		float toRemove = 0;

		if (playerMP.abilities.isFlying)
		{
			toRemove = 0.32F;
		}

		if (stack.getTag().getInt(TAG_MODE) == 2)
		{
			toRemove = 0.32F;
		}
		else if (stack.getTag().getInt(TAG_MODE) == 3)
		{
			toRemove = 0.64F;
		}

		removeEmc(stack, toRemove);

		playerMP.fallDistance = 0;
	}

	private boolean isFlyingEnabled(ItemStack stack)
	{
		return stack.getTag().getInt(TAG_MODE) == 1 || stack.getTag().getInt(TAG_MODE)== 3;
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int invSlot, boolean isHeldItem)
	{
		if (invSlot > 8 || !(entity instanceof PlayerEntity))
		{
			return;
		}
		tick(stack, ((PlayerEntity) entity));
	}
	
	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, @Nonnull Hand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		if (!world.isRemote)
		{
			int newMode = 0;

            switch (stack.getOrCreateTag().getInt(TAG_MODE))
			{
				case 0:
					newMode = 2;
					break;
				case 1:
					newMode = 3;
					break;
				case 2:
					newMode = 0;
					break;
				case 3:
					newMode = 1;
					break;
			}
			
			changeMode(player, stack, newMode);
		}
		return ActionResult.newResult(ActionResultType.SUCCESS, stack);
	}

	/**
	 * Change the mode of SWRG. Modes:<p>
	 * 0 = Ring Off<p>  
	 * 1 = Flight<p>
	 * 2 = Shield<p>
	 * 3 = Flight + Shield<p>
	 */
	public void changeMode(PlayerEntity player, ItemStack stack, int mode)
	{
		int oldMode = stack.getOrCreateTag().getInt(TAG_MODE);
		if (mode == oldMode)
		{
			return;
		}
		stack.getTag().putInt(TAG_MODE, mode);
		if (player == null)
		{
			//Don't do sounds if the player is null
			return;
		}
		if (mode == 0 || oldMode == 3)
		{
			//At least one mode deactivated
			player.getEntityWorld().playSound(null, player.posX, player.posY, player.posZ, PESounds.HEAL, SoundCategory.PLAYERS, 0.8F, 1.0F);
		}
		else if (oldMode == 0 || mode == 3)
		{
			//At least one mode activated
			player.getEntityWorld().playSound(null, player.posX, player.posY, player.posZ, PESounds.UNCHARGE, SoundCategory.PLAYERS, 0.8F, 1.0F);
		}
		//Doesn't handle going from mode 1 to 2 or 2 to 1
	}

	@Override
	public boolean canProvideFlight(ItemStack stack, ServerPlayerEntity player)
	{
		// Dummy result - swrg needs special-casing
		return false;
	}
	
	@Override
	public boolean showDurabilityBar(ItemStack stack)
	{
		return false;
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos)
	{
		if (!world.isRemote && ProjectEConfig.pedestalCooldown.swrg.get() != -1)
		{
			TileEntity te = world.getTileEntity(pos);
			if(!(te instanceof DMPedestalTile))
			{
				return;
			}
			DMPedestalTile tile = (DMPedestalTile) te;
			if (tile.getActivityCooldown() <= 0)
			{
				List<MobEntity> list = world.getEntitiesWithinAABB(MobEntity.class, tile.getEffectBounds());
				for (MobEntity living : list)
				{
					if (living instanceof TameableEntity && ((TameableEntity) living).isTamed())
					{
						continue;
					}
					world.addWeatherEffect(new LightningBoltEntity(world, living.posX, living.posY, living.posZ, false));
				}
				tile.setActivityCooldown(ProjectEConfig.pedestalCooldown.swrg.get());
			}
			else
			{
				tile.decrementActivityCooldown();
			}
		}
	}

	@Nonnull
	@Override
	public List<ITextComponent> getPedestalDescription()
	{
		List<ITextComponent> list = new ArrayList<>();
		if (ProjectEConfig.pedestalCooldown.swrg.get() != -1)
		{
			list.add(new TranslationTextComponent("pe.swrg.pedestal1").applyTextStyle(TextFormatting.BLUE));
			list.add(new TranslationTextComponent("pe.swrg.pedestal2", MathUtils.tickToSecFormatted(ProjectEConfig.pedestalCooldown.swrg.get())).applyTextStyle(TextFormatting.BLUE));
		}
		return list;
	}

	@Override
	public boolean shootProjectile(@Nonnull PlayerEntity player, @Nonnull ItemStack stack, @Nullable Hand hand)
	{
		EntitySWRGProjectile projectile = new EntitySWRGProjectile(player, false, player.world);
		projectile.shoot(player, player.rotationPitch, player.rotationYaw, 0, 1.5F, 1);
		player.world.spawnEntity(projectile);
		return true;
	}
}

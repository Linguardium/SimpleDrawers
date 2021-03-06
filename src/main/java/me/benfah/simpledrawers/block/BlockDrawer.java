package me.benfah.simpledrawers.block;

import me.benfah.simpledrawers.api.border.Border;
import me.benfah.simpledrawers.api.border.BorderRegistry;
import me.benfah.simpledrawers.api.border.BorderRegistry.BorderProperty;
import me.benfah.simpledrawers.api.drawer.BlockEntityAbstractDrawer;
import me.benfah.simpledrawers.api.drawer.DrawerType;
import me.benfah.simpledrawers.block.entity.BlockEntityBasicDrawer;
import me.benfah.simpledrawers.item.DrawerInteractable;
import me.benfah.simpledrawers.utils.BlockUtils;
import me.benfah.simpledrawers.utils.model.BorderModelProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class BlockDrawer extends BlockWithEntity implements InventoryProvider, BorderModelProvider
{

	public static DirectionProperty FACING = HorizontalFacingBlock.FACING;
	public static BorderProperty BORDER_TYPE = BorderRegistry.BORDER_TYPE;
	public static EnumProperty<DrawerType> DRAWER_TYPE = DrawerType.DRAWER_TYPE;

	public Identifier borderIdentifier;

	public BlockDrawer(Settings settings, Border border)
	{
		super(settings);
		this.setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(BORDER_TYPE, border).with(DRAWER_TYPE,
				DrawerType.FULL));
	}

	@Override
	public BlockEntity createBlockEntity(BlockView arg0)
	{
		return new BlockEntityBasicDrawer();
	}

	public BlockState rotate(BlockState state, BlockRotation rotation)
	{
		return (BlockState) state.with(FACING, rotation.rotate((Direction) state.get(FACING)));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
			BlockHitResult hit)
	{
		if (!world.isClient)
		{
			if (hand.equals(Hand.MAIN_HAND) && !player.getMainHandStack().isEmpty()
					&& state.get(FACING).equals(hit.getSide()))
			{
				BlockEntityAbstractDrawer drawer = (BlockEntityAbstractDrawer) world.getBlockEntity(pos);
				Vec2f interactPos = BlockUtils.getCoordinatesFromHitResult(hit);
				if (player.getMainHandStack().getItem() instanceof DrawerInteractable)
				{
					((DrawerInteractable) player.getMainHandStack().getItem()).interact(drawer, player,
							drawer.getItemHolderAt(interactPos.x, interactPos.y));
					return ActionResult.SUCCESS;
				}

				return drawer.getItemHolderAt(interactPos.x, interactPos.y).offer(player.getMainHandStack());
			}
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player)
	{
		if (!world.isClient)
		{
			BlockHitResult result = rayTrace(player);

			if (!result.getSide().equals(state.get(FACING)))
				return;

			Vec2f interactPos = BlockUtils.getCoordinatesFromHitResult(result);
			BlockEntityAbstractDrawer blockEntity = (BlockEntityAbstractDrawer) world.getBlockEntity(pos);

			blockEntity.getItemHolderAt(interactPos.x, interactPos.y).tryInsertIntoInventory(player,
					!player.isSneaking());
		}
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player)
	{
		BlockEntityAbstractDrawer drawer = (BlockEntityAbstractDrawer) world.getBlockEntity(pos);

		if (!player.isCreative())
		{
			ItemEntity drawerEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
					getStack(this, state.get(BORDER_TYPE)));
			drawerEntity.setToDefaultPickupDelay();
			world.spawnEntity(drawerEntity);
		}

		drawer.getItemHolders().stream().filter((holder) -> !holder.isEmpty()).forEach((holder) ->
		{
			ItemStack stack = new ItemStack(holder.getItemType(), holder.getAmount());
			if (holder.getTag() != null)
				stack.setTag(holder.getTag());

			ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack);
			itemEntity.setToDefaultPickupDelay();
			world.spawnEntity(itemEntity);
		});

		super.onBreak(world, pos, state, player);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack)
	{
		if (itemStack != null && !itemStack.equals(ItemStack.EMPTY))
			state = deserializeStack(itemStack, state);

		world.setBlockState(pos, state);

		super.onPlaced(world, pos, state, placer, itemStack);
	}

	@Override
	public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos)
	{
		BlockHitResult result = rayTrace(player);

		if (result.getSide().equals(state.get(FACING)) && !(player.getMainHandStack().getItem() instanceof AxeItem))
			return 0;

		return super.calcBlockBreakingDelta(state, player, world, pos);
	}

	public BlockRenderType getRenderType(BlockState state)
	{
		return BlockRenderType.MODEL;
	}

	public BlockState mirror(BlockState state, BlockMirror mirror)
	{
		return state.rotate(mirror.getRotation((Direction) state.get(FACING)));
	}

	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return (BlockState) this.getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite());
	}

	@Override
	public void appendProperties(Builder<Block, BlockState> builder)
	{
		builder.add(FACING).add(BORDER_TYPE).add(DRAWER_TYPE);
	}

	@Override
	public SidedInventory getInventory(BlockState state, IWorld world, BlockPos pos)
	{
		return ((BlockEntityAbstractDrawer) world.getBlockEntity(pos)).getInventoryHandler();
	}

	@Override
	public Identifier getBorderModel()
	{
		return borderIdentifier;
	}

	private BlockHitResult rayTrace(PlayerEntity player)
	{
		return (BlockHitResult) player.rayTrace(4.5, 0, false);
	}

	private static BlockState deserializeStack(ItemStack stack, BlockState state)
	{
		DeserializedInfo info = deserializeInfo(stack);

		if (info.getBorder() != null)
			state = state.with(BORDER_TYPE, info.getBorder());
		return state;
	}

	public static DeserializedInfo deserializeInfo(ItemStack stack)
	{
		if (stack.getSubTag("DrawerInfo") != null)
		{
			CompoundTag data = stack.getSubTag("DrawerInfo");

			String border = data.getString("Border");
			if (border != null)
			{
				Border b = BorderRegistry.getBorder(border);
				return new DeserializedInfo(b);
			}
		}
		return new DeserializedInfo(null);
	}

	public static ItemStack getStack(BlockDrawer drawer, Border border)
	{
		ItemStack result = new ItemStack(drawer.asItem());
		CompoundTag data = new CompoundTag();

		data.putString("Border", BorderRegistry.getName(border));

		result.putSubTag("DrawerInfo", data);
		return result;
	}

	public static class DeserializedInfo
	{

		private Border border;

		private DeserializedInfo(Border border)
		{
			this.border = border;
		}

		public Border getBorder()
		{
			return border;
		}

	}

}

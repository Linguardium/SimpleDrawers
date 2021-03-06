package me.benfah.simpledrawers.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ModelUtils
{
	public static Random RANDOM = new Random();

	public static void loadSpecialModels()
	{	
		
		ModelLoadingRegistry.INSTANCE.registerAppender((manager, out) ->
		{
			for (SpecialModel model : SpecialModel.values())
				out.accept(model.getIdentifier());
		});
		
		ModelLoadingRegistry.INSTANCE.registerVariantProvider(manager -> ((modelId, context) ->
		{
			for (SpecialModel m : SpecialModel.values())
				if (modelId.equals(m.getIdentifier()))
					return context.loadModel(new Identifier(modelId.getNamespace(), modelId.getPath()));
			return null;
		}));
		
		
	}
	
	public static void drawSpecialTexture(MatrixStack matrices, VertexConsumerProvider consumers, BakedModel model,
			int light, int overlay)
	{
		renderQuads(matrices.peek(), consumers.getBuffer(RenderLayer.getTranslucent()),
				model.getQuads(null, null, ModelUtils.RANDOM), light, overlay);

	}

	private static void renderQuads(MatrixStack.Entry entry, VertexConsumer vertexConsumer, List<BakedQuad> list,
			int light, int overlay)
	{
		for (BakedQuad quad : list)
			vertexConsumer.quad(entry, quad, 1F, 1F, 1F, light, overlay);
	}

	public enum SpecialModel
	{

		LOCK(new ModelIdentifier("simpledrawers:attributes/lock"));
		
		

		
		
		private ModelIdentifier identifier;

		private SpecialModel(ModelIdentifier identifier)
		{
			this.identifier = identifier;
		}

		public ModelIdentifier getIdentifier()
		{
			return identifier;
		}

		public BakedModel getBakedModel()
		{
			return MinecraftClient.getInstance().getBakedModelManager().getModel(identifier);
		}

	}
	
	public static boolean identifiersEqual(Identifier id1, Identifier id2)
	{
		return id1.getNamespace().equals(id2.getNamespace()) && id1.getPath().equals(id2.getPath());
	}
	
	public static String variantMapToString(Map<String, String> map)
	{
		String result = "";
		
		Iterator<Entry<String, String>> iterator = map.entrySet().iterator();
		
		while(iterator.hasNext())
		{
			Entry<String, String> entry = iterator.next();
			result = result + entry.getKey() + "=" + entry.getValue();
			
			if(iterator.hasNext())
				result = result + ",";
		}
		return result;
	}

}

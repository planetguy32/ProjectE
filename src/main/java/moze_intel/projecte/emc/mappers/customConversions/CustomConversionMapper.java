package moze_intel.projecte.emc.mappers.customConversions;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import moze_intel.projecte.PECore;
import moze_intel.projecte.emc.json.NSSItem;
import moze_intel.projecte.emc.json.NSSTag;
import moze_intel.projecte.emc.json.NormalizedSimpleStack;
import moze_intel.projecte.emc.collector.IMappingCollector;
import moze_intel.projecte.emc.mappers.IEMCMapper;
import moze_intel.projecte.emc.mappers.customConversions.json.ConversionGroup;
import moze_intel.projecte.emc.mappers.customConversions.json.CustomConversion;
import moze_intel.projecte.emc.mappers.customConversions.json.CustomConversionDeserializer;
import moze_intel.projecte.emc.mappers.customConversions.json.CustomConversionFile;
import moze_intel.projecte.emc.mappers.customConversions.json.FixedValues;
import moze_intel.projecte.emc.mappers.customConversions.json.FixedValuesDeserializer;
import net.minecraft.item.Item;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

public class CustomConversionMapper implements IEMCMapper<NormalizedSimpleStack, Long>
{
	public static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(CustomConversion.class, new CustomConversionDeserializer())
			.registerTypeAdapter(FixedValues.class, new FixedValuesDeserializer())
			.registerTypeAdapter(NormalizedSimpleStack.class, NormalizedSimpleStack.Serializer.INSTANCE)
			.setPrettyPrinting()
			.create();

	@Override
	public String getName()
	{
		return "CustomConversionMapper";
	}

	@Override
	public String getDescription()
	{
		return "Loads json files within datapacks (data/<domain>/pe_custom_conversions/*.json) to add values and conversions";
	}

	@Override
	public boolean isAvailable()
	{
		return true;
	}

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> mapper, CommentedFileConfig config, IResourceManager resourceManager)
	{
		Map<ResourceLocation, CustomConversionFile> files = load(resourceManager);
		for (CustomConversionFile file : files.values())
		{
			addMappingsFromFile(file, mapper);
		}
	}

	private static Map<ResourceLocation, CustomConversionFile> load(IResourceManager resourceManager)
	{
		Map<ResourceLocation, CustomConversionFile> loading = new HashMap<>();

		String folder = "pe_custom_conversions";
		String extension = ".json";

		// Find all data/<domain>/pe_custom_conversions/foo/bar.json
		for (ResourceLocation file : resourceManager.getAllResourceLocations(folder, n -> n.endsWith(extension)))
		{
			// <domain>:foo/bar
			ResourceLocation conversionId = new ResourceLocation(file.getNamespace(), file.getPath().substring(folder.length() + 1, file.getPath().length() - extension.length()));

			PECore.LOGGER.info("Considering file {}, ID {}", file, conversionId);

			// Iterate through all copies of this conversion, from lowest to highest priority datapack, merging the results together
			try {
				for (IResource resource : resourceManager.getAllResources(file))
				{
					CustomConversionFile result;
					try {
						result = parseJson(new InputStreamReader(resource.getInputStream()));
					} catch (JsonParseException ex) {
						PECore.LOGGER.error("Malformed JSON", ex);
						continue;
					}
					loading.merge(conversionId, result, CustomConversionFile::merge);
					IOUtils.closeQuietly(resource);
				}
			} catch (IOException e) {
				PECore.LOGGER.error("Could not load resource {}", file);
				e.printStackTrace();
			}
		}

		return loading;
	}

	private static void addMappingsFromFile(CustomConversionFile file, IMappingCollector<NormalizedSimpleStack, Long> mapper) {
		for (Map.Entry<String, ConversionGroup> entry : file.groups.entrySet())
		{
			PECore.debugLog("Adding conversions from group '{}' with comment '{}'", entry.getKey(), entry.getValue().comment);
			for (CustomConversion conversion : entry.getValue().conversions)
			{
				mapper.addConversion(conversion.count, conversion.output, conversion.ingredients);
			}
		}

		for (Map.Entry<NormalizedSimpleStack, Long> entry : file.values.setValueBefore.entrySet())
		{
			NormalizedSimpleStack something = entry.getKey();
			mapper.setValueBefore(something, entry.getValue());
			if (something instanceof NSSTag)
			{
				for (Item item : ((NSSTag) something).getAllElements())
				{
					mapper.setValueBefore(new NSSItem(item), entry.getValue());
				}
			}
		}

		for (Map.Entry<NormalizedSimpleStack, Long> entry : file.values.setValueAfter.entrySet())
		{
			NormalizedSimpleStack something = entry.getKey();
			mapper.setValueAfter(something, entry.getValue());
			if (something instanceof NSSTag)
			{
				for (Item item : ((NSSTag) something).getAllElements())
				{
					mapper.setValueAfter(new NSSItem(item), entry.getValue());
				}
			}
		}

		for (CustomConversion conversion : file.values.conversion)
		{
			NormalizedSimpleStack out = conversion.output;
			if (conversion.propagateTags && out instanceof NSSTag)
			{
				for (Item item : ((NSSTag) out).getAllElements())
				{
					mapper.setValueFromConversion(conversion.count, new NSSItem(item), conversion.ingredients);
				}
			}
			mapper.setValueFromConversion(conversion.count, out, conversion.ingredients);
		}
	}

	public static CustomConversionFile parseJson(Reader json) {
		return GSON.fromJson(new BufferedReader(json), CustomConversionFile.class);
	}
}

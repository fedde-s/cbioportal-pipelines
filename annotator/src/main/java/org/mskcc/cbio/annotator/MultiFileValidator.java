package org.mskcc.cbio.annotator;

import org.mskcc.cbio.oncotator.MultiFileAnnotator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Performs a simple validation for the annotated files.
 *
 * @author Selcuk Onur Sumer
 */
public class MultiFileValidator extends MultiFileAnnotator
{
	/**
	 * Driver method.
	 *
	 * @param sourceDir main source directory for input MAFs
	 * @param targetDir main output directory for output MAFs
	 */
	public void annotate(String sourceDir, String targetDir) throws IOException
	{
		this.sourceDir = sourceDir;
		this.targetDir = targetDir;

		List<File> inputMafList = this.getMafFiles(sourceDir);
		Map<File, File> map = this.makeOutputDirs(inputMafList, sourceDir, targetDir);
		this.annotateAll(map);
	}

	/**
	 * Validates all output MAF files within the given map.
	 *
	 * @param map   map of input MAF files to output directories
	 */
	protected void annotateAll(Map<File, File> map)
	{
		for (File file : map.keySet())
		{
			File outDir = map.get(file);
			File outputMaf = new File(outDir.getAbsolutePath() + "/" + file.getName());

			if (!outputMaf.exists())
			{
				System.out.println(file.getAbsolutePath() +
				                   ": Output file cannot be created");
			}
			else if (this.outputDiffers(file, outputMaf))
			{
				System.out.println(file.getAbsolutePath() +
				                   ": Number of data lines in the output file differ from the input");
			}
			else
			{
				System.out.println(file.getAbsolutePath() +
				                   ": Annotation status OK");
			}
		}
	}

	protected boolean outputDiffers(File input, File output)
	{
		int diff = AnnotateTool.compareFiles(input.getAbsolutePath(), output.getAbsolutePath());

		return !(diff == 0);
	}
}
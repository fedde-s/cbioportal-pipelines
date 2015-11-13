package org.mskcc.cbio.annotator;

import org.mskcc.cbio.maf.MafSanitizer;
import org.mskcc.cbio.oncotator.MultiFileAnnotator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Sanitizes critical fields of the input MAF file.
 *
 * @author Selcuk Onur Sumer
 */
public class MultiFileSanitizer extends MultiFileAnnotator
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

		List<File> inputMafList = this.getMafFiles(sourceDir);
		this.sanitizeAll(inputMafList);
	}

	protected void annotateAll(Map<File, File> map)
	{
		// empty implementation...
	}

	/**
	 * Validates all output MAF files within the given map.
	 *
	 * @param list   list of input MAF files
	 */
	protected void sanitizeAll(List<File> list) throws IOException
	{
		for (File file : list)
		{
			System.out.println("FILE: " + file.getAbsolutePath());
			MafSanitizer sanitizer = new MafSanitizer();
			sanitizer.sanitizeMaf(file.getAbsolutePath());
		}
	}
}

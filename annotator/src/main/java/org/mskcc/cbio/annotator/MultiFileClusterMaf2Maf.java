package org.mskcc.cbio.annotator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Designed to oncotate all MAF files (with Maf2Maf tool)
 * within a given directory by using "cluster" mode.
 *
 * @author Selcuk Onur Sumer
 */
public class MultiFileClusterMaf2Maf extends MultiFileMaf2Maf
{
	public MultiFileClusterMaf2Maf(AnnotatorConfig config)
	{
		super(config);
	}

	public MultiFileClusterMaf2Maf()
	{
		super();
	}

	/**
	 * Annotates all input MAF files within the given map. Writes output
	 * MAFs to the mapped directory.
	 *
	 * @param map   map of input MAF files to output directories
	 */
	protected void annotateAll(Map<File, File> map)
	{
		List<Thread> threads = new ArrayList<Thread>();

		for (File file : map.keySet())
		{
			File outDir = map.get(file);
			String inputMaf = file.getAbsolutePath();
			String outputMaf = outDir.getAbsolutePath() + "/" + file.getName();
			String cache = outDir.getAbsolutePath() + "/anno_files";

			AnnotatorConfig config = this.config.copy();

			config.setMode("cluster");
			config.setInput(inputMaf);
			config.setOutput(outputMaf);
			config.setClusterLog(outputMaf + ".bsub.log");

			// override default tmp dir and intermediate file,
			// it may cause problems in cluster mode
			config.setIntermediateMaf(outputMaf + ".tmp");
			config.setTmpDir(cache);

			// run each in a separate thread

			Thread annotator = new BsubThread(config);
			threads.add(annotator);
			annotator.start();
		}

		for(Thread t: threads)
		{
			try {
				t.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected class BsubThread extends Thread {

		private AnnotatorConfig config;

		public BsubThread(AnnotatorConfig config)
		{
			this.config = config;
		}

		public void run()
		{
			int result = AnnotateTool.driver(this.config);

			if (result != 0)
			{
				System.out.println("[ERROR] Process completed with exit code " + result);
			}
		}
	}
}

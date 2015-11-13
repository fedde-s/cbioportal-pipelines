package org.mskcc.cbio.maf;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestMafMerger extends TestCase
{
	public void testMerger()
	{
		// TODO replace with getResourceStream()
		String target = "target/test-classes/maf_annotated.txt";
		String source = "target/test-classes/maf_supp.txt";

		try
		{
			// run sanitizer
			MafMerger merger = new MafMerger();
			File merged = merger.merge(new File(target), new File(source));

			// check if everything is ok with the output

			BufferedReader reader = new BufferedReader(new FileReader(merged));
			MafHeaderUtil headerUtil = new MafHeaderUtil();

			String line = headerUtil.extractHeader(reader);
			MafUtil util = new MafUtil(line);

			// assert the number of columns
			// (32 standard + 1 annotated + 1 custom from annotated + 1 custom from supp)
			assertEquals(35, util.getHeaderCount());

			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}

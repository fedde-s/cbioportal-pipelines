package org.mskcc.cbio.maf;

public class MafErrorReport
{
	// TODO add error report for sample barcode

	public int lineNumber = -1;
	public boolean malformedAllele = false;
	public boolean malformedRef = false;
	public boolean malformedTum1 = false;
	public boolean malformedTum2 = false;
	public boolean missingEnd = false;
	public boolean missingStart = false;
	public boolean invalidChromosome = false;
	public boolean longChrName = false;
	public boolean nonAdjacentIns = false;
	public boolean invalidLength = false;
	
	public boolean containsError()
	{
		return (this.malformedAllele ||
			this.invalidChromosome ||
			this.longChrName ||
			this.missingStart ||
			this.missingEnd ||
			this.nonAdjacentIns ||
			this.invalidLength);
	}

	public boolean hasCriticalError()
	{
		return (this.malformedRef ||
		        (this.malformedTum1 && this.malformedTum2) ||
		        this.invalidChromosome ||
		        this.missingStart);
	}

	public boolean isInvalidPos()
	{
		// check for invalidity of both start & end positions
		return this.missingStart && this.missingEnd;
	}
}

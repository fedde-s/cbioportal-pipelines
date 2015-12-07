#! /usr/bin/env python

# ------------------------------------------------------------------------------
# imports

import sys
import itertools
import getopt
import os
import shutil

# ------------------------------------------------------------------------------
# globals

# some file descriptors
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

GENOMIC_BUILD_COUNTERPART = 'hg19'

SEG_FILE_PATTERN = '_cna_' + GENOMIC_BUILD_COUNTERPART + '.seg'
SEG_META_PATTERN = 'meta_cna_' + GENOMIC_BUILD_COUNTERPART + '_seg'

MUTATION_FILE_PATTERN = '_mutations_extended'
MUTATION_META_PATTERN = 'meta_mutations_extended'

CNA_FILE_PATTERN = '_CNA'
CNA_META_PATTERN = 'meta_CNA'

CLINICAL_FILE_PATTERN = '_clinical'
CLINICAL_META_PATTERN = 'meta_clinical'

LOG2_FILE_PATTERN = '_log2CNA'
LOG2_META_PATTERN = 'meta_log2CNA'

EXPRESSION_FILE_PATTERN = '_expression'
EXPRESSION_META_PATTERN = 'meta_expression'

FUSION_FILE_PATTERN = '_fusions'
FUSION_META_PATTERN = 'meta_fusions'

METHYLATION450_FILE_PATTERN = '_methylation_hm450'
METHYLATION450_META_PATTERN = 'meta_methylation_hm450'

METHYLATION27_FILE_PATTERN = '_methylation_hm27'
METHYLATION27_META_PATTERN = 'meta_methylation_hm27'

RPPA_FILE_PATTERN = '_rppa'
RPPA_META_PATTERN = 'meta_rppa'

TIMELINE_FILE_PATTERN = '_timeline_'
TIMELINE_META_PATTERN = 'meta_timeline'

CNA_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

MUTATION_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

SEG_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description',
    'reference_genome_id',
    'data_filename',
    'description'
]

LOG2_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

EXPRESSION_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

METHYLATION450_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

METHYLATION27_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

FUSION_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

RPPA_META_FIELDS = [
    'cancer_study_identifier',
    'genetic_alteration_type',
    'datatype',
    'stable_id',
    'show_profile_in_analysis_tab',
    'profile_name',
    'profile_description'
]

# only files fitting patterns placed in these two lists will be merged
NORMAL_MERGE_PATTERNS = [SEG_META_PATTERN]

CNA_MERGE_PATTERNS = [CNA_META_PATTERN,
	LOG2_META_PATTERN,
	EXPRESSION_META_PATTERN,
	FUSION_META_PATTERN,
	METHYLATION27_META_PATTERN,
	METHYLATION450_META_PATTERN,
	RPPA_META_PATTERN]

META_FILE_MAP = {MUTATION_META_PATTERN:(MUTATION_FILE_PATTERN,MUTATION_META_FIELDS,'mutations'),
	CNA_META_PATTERN:(CNA_FILE_PATTERN,CNA_META_FIELDS,'cna'),
	LOG2_META_PATTERN:(LOG2_FILE_PATTERN,LOG2_META_FIELDS,'log2CNA'),
	SEG_META_PATTERN:(SEG_FILE_PATTERN,SEG_META_FIELDS,''),
	METHYLATION27_META_PATTERN:(METHYLATION27_META_PATTERN,METHYLATION27_META_FIELDS,'methylation_hm27'),
	METHYLATION450_META_PATTERN:(METHYLATION450_META_PATTERN,METHYLATION450_META_FIELDS,'methylation_hm450'),
	FUSION_META_PATTERN:(FUSION_FILE_PATTERN,FUSION_META_FIELDS,'mutations'),
	RPPA_META_PATTERN:(RPPA_FILE_PATTERN,RPPA_META_FIELDS,'rppa'),
	EXPRESSION_META_PATTERN:(EXPRESSION_FILE_PATTERN,EXPRESSION_META_FIELDS,'')}

# ------------------------------------------------------------------------------
# Functions

# ------------------------------------------------------------------------------
# prepare merge
def mergeStudies(study1files,study2files,outputdirectory,studyid):
	allPatterns = NORMAL_MERGE_PATTERNS + CNA_MERGE_PATTERNS
	filesTouched = []
	for pat in allPatterns:
		for s1 in study1files:
			for s2 in study2files:
				if pat in s1 and pat in s2:
					s1data,s2data = getDataFromMeta(study1files,study2files,pat)
					filesTouched.extend([s1,s2,s1data,s2data])
					mergeFiles(s1data,s2data,s1,s2,outputdirectory,pat,studyid)

	copyNonMerged(study1files,study2files,outputdirectory,filesTouched,studyid)


# ------------------------------------------------------------------------------
# Merge 
def mergeFiles(s1File,s2File,s1,s2,outputdirectory,pattern,studyid):
	newheader = []
	firstLine = True

	try:
		file1 = open(s1File,'rU')
		file2 = open(s2File,'rU')
	except IOError:
		print >> ERROR_FILE, 'One of the following files does not exist:\n' + \
			'\t' + s1File + '\n' + \
			'\t' + s2File + '\n' + \
			'\tMake sure files are named correctly: https://github.com/cBioPortal/cbioportal/wiki/File-Formats'
		sys.exit(1)

	if not 'seg' in pattern:
		outputfile = open(outputdirectory+'/data'+META_FILE_MAP[pattern][0]+'.txt','w')
	else:
		outputfile = open(outputdirectory+'/data'+META_FILE_MAP[pattern][0],'w')

	# key: String:gene
	# value: [(sample1Name,sample1Value),(sample1Name,sample2Value), ... ]
	geneSampleDict = {}

	# Go through every line in both files, put data in the correct spots, write them out
	for l1,l2 in itertools.izip_longest(file1,file2):
		if firstLine:
			newheader,header1,header2 = mergeHeader(l1,l2,outputfile)
			firstLine = False
		else:
			if l1 is not None:
				if pattern in CNA_MERGE_PATTERNS:
					geneSampleDict = cnaRow(l1,header1,geneSampleDict)
				elif pattern in NORMAL_MERGE_PATTERNS:
					writeRow(l1,header1,newheader,outputfile)
			if l2 is not None:
				if pattern in CNA_MERGE_PATTERNS:
					geneSampleDict = cnaRow(l2,header2,geneSampleDict)
				elif pattern in NORMAL_MERGE_PATTERNS:
					writeRow(l2,header2,newheader,outputfile)

	# actually writes out to the file for CNA-style data
	if pattern in CNA_MERGE_PATTERNS:
		writeCNA(geneSampleDict,outputfile,newheader)

	makeMeta(outputdirectory,studyid,s1,s2=s2,pattern=pattern)

	# close up
	file1.close()
	file2.close()
	outputfile.close()

# ------------------------------------------------------------------------------
# Creates the newheader list, and header1 and header2 lists.
# Writes the new merged header to file

def mergeHeader(l1,l2,outputfile):
	header1 = map(str.strip,l1.split('\t'))
	header2 = map(str.strip,l2.split('\t'))

	newheader = header1

	for h in header2:
		if h not in newheader:
			newheader.append(h)

	outputfile.write('\t'.join(newheader) + '\n')

	return newheader,header1,header2

# ------------------------------------------------------------------------------
# Matches data row index to header of its own file, then to the newheader, where it assigns that data item to the correct index
# Then writes new row to file (essentially rearranges the data, ensuring it's in the correct index)
def writeRow(line,header,newheader,outputfile):
	rowIndexMap = {}
	data = map(str.strip,line.split('\t'))

	for i,d in enumerate(data):
		h = header[i]
		hindex = newheader.index(h)
		rowIndexMap[hindex] = d

	# TODO: better way to do this - see writeCNA
	rowlist = []

	for i in range(len(newheader)):
		rowlist.append(rowIndexMap.get(i,'NA'))

	for i,item in enumerate(rowlist):
		if item == '':
		 	rowlist[i] = 'NA'

	outputfile.write('\t'.join(rowlist) + '\n')

# ------------------------------------------------------------------------------
# Adds a row to the gene-sample map. 
def cnaRow(line,header,geneSampleDict):
	data = map(str.strip,line.split('\t'))
	
	# assuming that the gene should be in the first column
	gene = data[0]

	for i,d in enumerate(data):
		sample = header[i]
		if gene in geneSampleDict:
			geneSampleDict[gene].append((sample,d))
		else:
			geneSampleDict[gene] = [(sample,d)]

	return geneSampleDict

# ------------------------------------------------------------------------------
# writes data from the geneSampleDict to file by going through each gene and mapping 
def writeCNA(geneSampleDict,outputfile,newheader):
	for gene,samples in geneSampleDict.iteritems():
		newrow = ['NA'] * len(newheader)
		newrow[0] = gene

		for sample in samples:
			index = newheader.index(sample[0])
			newrow[index] = sample[1]

		outputfile.write('\t'.join(newrow) + '\n')


# ------------------------------------------------------------------------------
# Check existence of study directory
def checkDirectories(directories,outputdirectory):
	for d in directories:
		if not os.path.exists(d):
			print >> ERROR_FILE, 'Study cannot be found: ' + d
			sys.exit(2)
	if not os.path.exists(outputdirectory):
		os.makedirs(outputdirectory)

# ------------------------------------------------------------------------------
# Find data file for given meta 
def getDataFromMeta(study1files,study2files,pattern):
	s1 = [x for x in study1files if META_FILE_MAP[pattern][0] in x and 'meta' not in x.lower()][0]
	s2 = [x for x in study2files if META_FILE_MAP[pattern][0] in x and 'meta' not in x.lower()][0]
	return s1,s2

# ------------------------------------------------------------------------------
# Copies all other files from study that were not merge-able (not existent in other study to merge) 
def copyNonMerged(s1files,s2files,outputdirectory,filesTouched,studyid):
	allFiles = s1files + s2files

	studyid1 = getCancerStudyId(s1files)
	studyid2 = getCancerStudyId(s2files)

	copyStudyFiles(s1files,outputdirectory,filesTouched,studyid,studyid1)
	copyStudyFiles(s2files,outputdirectory,filesTouched,studyid,studyid2)

# ------------------------------------------------------------------------------
# copies files for a single study
def copyStudyFiles(studyfiles,outputdirectory,filesTouched,studyid,substudyid):
	for f in studyfiles:
		if f not in filesTouched:
			fname = f.split('/')[-1]
			if 'meta' in fname:
				makeMeta(outputdirectory,studyid,f,substudyid=substudyid)
			elif not 'seg' in fname:
				newfilename = outputdirectory + '/' + fname.split('.')[0] + '_' + substudyid + '.txt'
				shutil.copy(f,newfilename)
			else:
				newfilename = outputdirectory + '/' + fname
				shutil.copy(f,newfilename)

# ------------------------------------------------------------------------------
# creates a meta file for merged files
def makeMeta(outputdirectory,studyid,s1,substudyid='',s2='',pattern=''):
	if not 'seg' in s1 and s2 == '':
		filename = outputdirectory + '/' + s1.split('/')[-1].split('.')[0] + '_' + substudyid + '.txt'
	else:
		filename = outputdirectory + '/' + s1.split('/')[-1]
	metaFile = open(filename,'w')
	meta1 = open(s1,'rU')
	metadata2 = {}
	if s2 != '':
		meta2 = open(s2,'rU')
		metadata2 = readMeta(meta2)
	metadata1 = readMeta(meta1)

	if pattern != '':
		for field in META_FILE_MAP[pattern][1]:
			if field == 'cancer_study_identifier':
				metaFile.write(field+':'+studyid+'\n')
			elif field == 'stable_id':
				metaFile.write(field+':'+studyid+'_'+META_FILE_MAP[pattern][2]+'\n')
			elif field == 'show_profile_in_analysis_tab':
				metaFile.write(field+':'+metadata1.get('show_profile_in_analysis_tab','')+'\n')
			elif field == 'profile_description':
				metaFile.write(field+':'+metadata1.get('cancer_study_identifier','')+' '+metadata1.get('profile_description','')+' '+metadata2.get('cancer_study_identifier','')+' '+metadata2.get('profile_description','')+'\n')
			elif field == 'datatype':
				metaFile.write(field+':'+metadata1.get('datatype','')+'\n')
			elif field == 'profile_name':
				metaFile.write(field+':'+metadata1.get('cancer_study_identifier','')+' '+metadata1.get('profile_name','')+' '+metadata2.get('cancer_study_identifier','')+' '+metadata2.get('profile_name','')+'\n')
			elif field in metadata1:
				metaFile.write(field+':'+metadata1.get(field,'')+'\n')
			elif field in metadata2:
				metaFile.write(field+':'+metadata2.get(field,'')+'\n')

	else:
		metaFile.write('cancer_study_identifier:'+studyid+'\n')
		metaFile.write('stable_id:'+studyid+'_'+metadata1.get('stable_id','').split('_')[-1]+'\n')
		for field,value in metadata1.iteritems():
			if field != 'cancer_study_identifier' and field != 'stable_id':
				metaFile.write(field+':'+value+'\n')

	metaFile.close()

# ------------------------------------------------------------------------------
# reads a metafile and puts contents into a dictionary
def readMeta(metafile):
	meta = {}
	for line in metafile:
		line = line.strip()
		linedata = line.split(':')
		meta[linedata[0]] = linedata [1]
	return meta

# ------------------------------------------------------------------------------
# gets the cancer study id given a set of study files. Uses first meta file it finds
def getCancerStudyId(studyfiles):
	for studyfile in studyfiles:
		if 'meta' in studyfile:
			f = open(studyfile,'rU')
			metadata = readMeta(f)
			try:
				return metadata['cancer_study_identifier'].strip()
			except KeyError:
				print >> ERROR_FILE, 'Meta file does not have cancer_study_identifier: ' + studyfile
				sys.exit(1)

def usage():
    print >> OUTPUT_FILE, 'merge.py --study1 [/path/to/study1] --study2 [/path/to/study2] --output-directory [/path/to/output] --study-id'


# ------------------------------------------------------------------------------
# main function - do a merge!
def main():
	# get command line stuff
	try:
		opts, args = getopt.getopt(sys.argv[1:], '', ['study1=', 'study2=', 'output-directory=','study-id='])
	except getopt.error, msg:
		print >> ERROR_FILE, msg
		usage()
		sys.exit(2)

	study1 = ''
	study2 = ''
	outputdirectory = ''
	studyid = ''
	cna = False

    # process the options
	for o, a in opts:
		if o == '--study1':
			study1 = a
		elif o == '--study2':
			study2 = a
		elif o == '--output-directory':
			outputdirectory = a
		elif o == '--study-id':
			studyid = a

	if (study1 == '' or study2 == '' or outputdirectory == '' or studyid == ''):
		usage()
		sys.exit(2)

	# check directories exist
	checkDirectories([study1,study2],outputdirectory)

	# get all the filenames
	study1_files = [study1 + '/' + x for x in os.listdir(study1)]
	study2_files = [study2 + '/' + x for x in os.listdir(study2)]

	# merge the studies
	mergeStudies(study1_files,study2_files,outputdirectory,studyid)


if __name__ == '__main__':
	main()

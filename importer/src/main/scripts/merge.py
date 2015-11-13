# ------------------------------------------------------------------------------
# imports

import sys
import itertools
import getopt
import os

# ------------------------------------------------------------------------------
# globals

# some file descriptors
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# ------------------------------------------------------------------------------
# Functions

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

	rowlist = []

	for i in range(len(newheader)):
		rowlist.append(rowIndexMap.get(i,'NA'))

	for i,item in enumerate(rowlist):
		if item == '':
		 	rowlist[i] = 'NA'

	outputfile.write('\t'.join(rowlist) + '\n')

def usage():
    print >> OUTPUT_FILE, 'merge.py --file1 [/path/to/file1] --file2 [/path/to/file2] --outputfile [/path/to/output]'


# ------------------------------------------------------------------------------
# main function - do a merge!
def main():
	# get command line stuff
	try:
		opts, args = getopt.getopt(sys.argv[1:], '', ['file1=', 'file2=', 'outputfile='])
	except getopt.error, msg:
		print >> ERROR_FILE, msg
		usage()
		sys.exit(2)

	filename1 = ''
	filename2 = ''
	outputfile = ''

    # process the options

	for o, a in opts:
		if o == '--file1':
			filename1 = a
		elif o == '--file2':
			filename2 = a
		elif o == '--outputfile':
			outputfile = a

	if (filename1 == '' or filename2 == '' or outputfile == ''):
		usage()
		sys.exit(2)

    # check existence of files
	if not os.path.exists(filename1):
		print >> ERROR_FILE, 'File1 cannot be found: ' + filename1
		sys.exit(2)
	if not os.path.exists(filename2):
		print >> ERROR_FILE, 'File2 cannot be found: ' + filename2
		sys.exit(2)

	file1 = open(filename1,'rU')
	file2 = open(filename2,'rU')

	outputfile = open(outputfile,'w')

	newheader = []
	firstLine = True

	# Go through every line in both files, put data in the correct spots, write them out
	for l1,l2 in itertools.izip_longest(file1,file2):
		if firstLine:
			newheader,header1,header2 = mergeHeader(l1,l2,outputfile)
			firstLine = False
		else:
			if l1 is not None:
				writeRow(l1,header1,newheader,outputfile)
			if l2 is not None:
				writeRow(l2,header2,newheader,outputfile)

if __name__ == '__main__':
	main()
#!/bin/bash

# folder structure to generate data sets
# - current directory
# --- 1 <= contain a single fast5 file
# --- 2
# --- fastq
#    --- 1 <= contain multiple fastq files
#    --- 2

DATASET_FOLDER=datasets
FASTQ_FOLDER=fastq
FAST5_FOLDER=fast5
FASTQ_FILENAME=reads.fastq

# make a folder to store generated data sets if not exists
mkdir -p $DATASET_FOLDER || die "Creating $DATASET_FOLDER folder failed"

# list all the folders in current directory and extract the fast5 folders
# numbered folders are fast5

for folder in $(ls)
do
  if [[ $folder =~ ^[0-9]+$ ]]
  	# found a numbered
	then
	    # echo $folder
	    # get the fast5 file inside the $folder
	    fast5file=$(ls $folder)
	    echo "Checking for fast5 files in folder $folder"
	    if [[ ${fast5file: -6} == ".fast5" ]]
	  	# found a fast5
		then
			
			count=`ls $FASTQ_FOLDER/$folder/*.fastq -1 2>/dev/null | wc -l`
			if [ $count != 0 ]
				then 
					# concatenate fastq files to a single fastq file
				    cat $FASTQ_FOLDER/$folder/*fastq > $FASTQ_FILENAME || die "Concatenating fastq files failed"

				    mkdir $DATASET_FOLDER/$folder || die "Creating $DATASET_FOLDER/$folder folder failed"

				    mv $FASTQ_FILENAME $DATASET_FOLDER/$folder || die "Moving $FASTQ_FILENAME to $DATASET_FOLDER/$folder folder failed"

				    mkdir $DATASET_FOLDER/$folder/$FAST5_FOLDER || die "Creating $DATASET_FOLDER/$folder/fast5 folder failed"

				    cp $folder/$fast5file $DATASET_FOLDER/$folder/$FAST5_FOLDER || die "Copying $folder/$fast5file $DATASET_FOLDER/$folder/fast5 folder failed"

				    cd $DATASET_FOLDER/$folder || die "Changing directory to $DATASET_FOLDER/$folder failed"

				    echo "Generating data set $folder.zip"

				    zip -r ../$folder.zip * || die "Zipping $folder.zip failed"

				    cd .. || die "Changing directory to $DATASET_FOLDER failed"

				    rm -r $folder || die "Removing $folder folder failed"

				    cd .. || die "Changing directory to Parent directory failed"
				else
					echo "No fastq files found in folder $FASTQ_FOLDER/$folder"
			fi
		else
			echo "Skipping folder $folder as there are no fast5 files"
	  	fi
  fi
done;
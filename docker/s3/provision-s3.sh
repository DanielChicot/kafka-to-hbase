#!/usr/bin/env bash

source ./environment.sh

aws_configure
aws_s3_mb kafka2s3
aws_s3_mb ucarchive
aws_s3_mb manifests
aws_s3_ls

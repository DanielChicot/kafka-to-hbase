#!/bin/bash

aws_configure() {
    aws configure set aws_access_key_id AWS_ACCESS_KEY_ID
    aws configure set aws_secret_access_key AWS_SECRET_ACCESS_KEY
    aws configure set default.region eu-west-2
    aws configure set region} eu-west-2
}

aws_local() {
    aws --endpoint-url http://aws-s3:4566 --region=eu-west-2 "$@"
}

aws_s3_mb() {
    local bucket_name=${1:?Usage: $FUNCNAME bucket-name}
    if ! aws_local s3 ls s3://$bucket_name 2>/dev/null; then
        echo Making \'$bucket_name\'.
        aws_local s3 mb "s3://$bucket_name"
        aws_local s3api put-bucket-acl --bucket "$bucket_name" --acl public-read
    else
        echo Bucket \'$bucket_name\' exists. >&2
    fi
}

aws_s3_ls() {
    aws_local s3 ls
}

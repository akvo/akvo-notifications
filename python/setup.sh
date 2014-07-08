#!/bin/bash

rm -rf virt_env/env
virtualenv virt_env/env --no-site-packages
source virt_env/env/bin/activate
pip install -r virt_env/requirements.txt

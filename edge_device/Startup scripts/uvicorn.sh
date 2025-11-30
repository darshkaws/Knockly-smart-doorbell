#!/bin/bash

cd /home/admin/Test_facial_rec/api

source venv/bin/activate

cd src

export JWT_SECRET=$(openssl rand -hex 32)

uvicorn main:app --host 0.0.0.0 --port 8000

#
# Copyright © 2019 admin (admin@infrastructurebuilder.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.38"
    }
  }
  backend "local" {
    path = "${datadirpath}/terraform.tfstate"
  }

}

# Configure the AWS Provider
provider "aws" {
  profile = "sharedservices.dev"
  region  = "us-east-1"
}


module "rcs3rsblogging" {
    source = "../../s3rsb-logbucket-module/src/main/terraform"
    costcenter = "devsecops"
    src = "a:b:c:123"
    environment = "abc"
    thisproject = "${thisproject}"
    datadirpath = "${datadirpath}"
}

output "rsbucket" {
    description = "Logging bucket"
    value = module.rcs3rsblogging.rsbucket
}
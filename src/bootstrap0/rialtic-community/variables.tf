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

variable "costcenter" {
    default = "admin"
    description = "Cost center of these artifacts"
    validation {
      condition = contains(["platform","reference-source-repo","security","admin","devsecops","shared-services"], var.costcenter)
      error_message = "The cost center is not a valid costcenter."
    }
}

variable "thisproject" {
    description = "The project to hold state for"
    type = string
}



variable "aws_region" {
  description = "AWS region where resources will be created."
  type        = string
  default     = "eu-west-1"
}

variable "project_name" {
  description = "Project name used for resource naming."
  type        = string
  default     = "paychecker"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "dev"
}

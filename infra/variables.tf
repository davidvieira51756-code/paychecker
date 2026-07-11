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

variable "vpc_cidr" {
  description = "CIDR block for the project Virtual Private Cloud."
  type        = string
  default     = "10.20.0.0/16"
}

variable "db_name" {
  description = "Initial PostgreSQL database name."
  type        = string
  default     = "paychecker_db"
}

variable "db_username" {
  description = "PostgreSQL master username."
  type        = string
  default     = "paychecker"
}

variable "db_password" {
  description = "PostgreSQL master password. Set this in terraform.tfvars and do not commit it."
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "Amazon Relational Database Service instance class."
  type        = string
  default     = "db.t4g.micro"
}

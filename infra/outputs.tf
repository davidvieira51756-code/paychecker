output "api_ecr_repository_name" {
  description = "Amazon Elastic Container Registry repository name for the backend API image."
  value       = aws_ecr_repository.api.name
}

output "api_ecr_repository_url" {
  description = "Amazon Elastic Container Registry repository URL for the backend API image."
  value       = aws_ecr_repository.api.repository_url
}

output "vpc_id" {
  description = "Virtual Private Cloud ID."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs for the future Application Load Balancer."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs for Amazon Relational Database Service."
  value       = aws_subnet.private[*].id
}

output "database_endpoint" {
  description = "Amazon Relational Database Service PostgreSQL endpoint."
  value       = aws_db_instance.postgres.endpoint
}

output "api_security_group_id" {
  description = "Security group ID for the future Amazon Elastic Container Service API tasks."
  value       = aws_security_group.api.id
}

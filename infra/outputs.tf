output "api_ecr_repository_name" {
  description = "Amazon Elastic Container Registry repository name for the backend API image."
  value       = aws_ecr_repository.api.name
}

output "api_ecr_repository_url" {
  description = "Amazon Elastic Container Registry repository URL for the backend API image."
  value       = aws_ecr_repository.api.repository_url
}

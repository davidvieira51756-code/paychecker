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

output "api_load_balancer_dns_name" {
  description = "Public DNS name of the Application Load Balancer for the backend API."
  value       = aws_lb.api.dns_name
}

output "api_health_url" {
  description = "Public health check URL for the backend API."
  value       = "http://${aws_lb.api.dns_name}/actuator/health"
}

output "ecs_cluster_name" {
  description = "Amazon Elastic Container Service cluster name."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "Amazon Elastic Container Service service name."
  value       = aws_ecs_service.api.name
}

output "frontend_bucket_name" {
  description = "Amazon Simple Storage Service bucket for frontend build artifacts."
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_cloudfront_domain_name" {
  description = "Amazon CloudFront domain name for the frontend."
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_cloudfront_distribution_id" {
  description = "Amazon CloudFront distribution ID for cache invalidations."
  value       = aws_cloudfront_distribution.frontend.id
}

output "frontend_url" {
  description = "Public frontend URL."
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "github_actions_role_arn" {
  description = "IAM role ARN used by GitHub Actions OIDC deployments."
  value       = aws_iam_role.github_actions.arn
}

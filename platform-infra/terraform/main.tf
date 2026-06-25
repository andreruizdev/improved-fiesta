terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}
provider "aws" {
  region = "us-east-1"
}
resource "aws_eks_cluster" "fiesta_mlops" {
  name     = "fiesta-mlops-cluster"
  role_arn = "arn:aws:iam::123456789012:role/eks-service-role-AWSServiceRoleForAmazonEKS-..."
  vpc_config {
    subnet_ids = ["subnet-12345", "subnet-67890"]
  }
}

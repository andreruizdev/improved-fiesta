terraform {
  required_version = ">= 1.5.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.90.0"
    }
  }
  # Remote backend ensures team-wide state concurrency and prevents state overwrites
  backend "azurerm" {
    resource_group_name  = "foundry-core-mgmt"
    storage_account_name = "foundrystateremote"
    container_name       = "tfstate"
    key                  = "prod.foundry-stream.tfstate"
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destruction = false
    }
  }
}
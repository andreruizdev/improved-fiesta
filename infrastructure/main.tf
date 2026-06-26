resource "azurerm_resource_group" "rg" {
  name     = "foundry-stream-prod-rg"
  location = "eastus2"
}

resource "azurerm_virtual_network" "vnet" {
  name                = "foundry-prod-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_subnet" "aks_subnet" {
  name                 = "aks-node-subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
  address_prefixes     = ["10.0.1.0/24"]
}

resource "azurerm_kubernetes_cluster" "aks" {
  name                = "foundry-engine-cluster"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  dns_prefix          = "foundrykube"

  default_node_pool {
    name       = "systempool"
    node_count = 3
    vm_size    = "Standard_D4s_v5" # Optimized for heavy I/O and container runtimes
    vnet_subnet_id = azurerm_subnet.aks_subnet.id
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "standard"
  }
}
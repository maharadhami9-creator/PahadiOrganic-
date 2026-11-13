package com.example.data.catalog

import com.example.data.model.OrganicProduct

object HarvestCatalog {
  val categories = listOf("All", "Honey & Ghee", "Grains & Pulses", "Teas & Herbs", "Oils & Elixirs")

  val regions = listOf("Kumaon", "Garhwal", "Kinnaur", "Spiti Valley", "Darjeeling Hills")

  val products =
    listOf(
      OrganicProduct(
        id = "wild_forest_honey",
        name = "Wild Himalayan Forest Honey",
        category = "Honey & Ghee",
        region = "Kumaon",
        altitude = "2,100m",
        unitLabel = "500g Glass Jar",
        priceInr = 650.0,
        description =
          "Raw, unheated multiflora honey harvested ethically from high-altitude rhododendron and wild thyme forests in Binsar.",
        harvestNote = "Autumn Flora Batch • Zero Sugar Syrup",
        isBestSeller = true,
      ),
      OrganicProduct(
        id = "badri_cow_ghee",
        name = "Artisanal Badri Cow Bilona Ghee",
        category = "Honey & Ghee",
        region = "Garhwal",
        altitude = "2,400m",
        unitLabel = "500ml Jar",
        priceInr = 1280.0,
        description =
          "Slow-churned cultured A2 ghee from free-grazing Badri cows nourished on alpine medicinal herbs in Chamoli pastures.",
        harvestNote = "Hand-Churned Wooden Bilona • Small Batch",
        isBestSeller = true,
      ),
      OrganicProduct(
        id = "kumaoni_red_rice",
        name = "Kumaoni Red Rice (Lal Chawal)",
        category = "Grains & Pulses",
        region = "Kumaon",
        altitude = "1,850m",
        unitLabel = "1 kg Linen Pack",
        priceInr = 280.0,
        description =
          "Nutty, iron-rich heirloom red rice grown in glacial spring-fed terraced fields without synthetic fertilizers.",
        harvestNote = "Single-Origin Rainfed Terrace Harvest",
      ),
      OrganicProduct(
        id = "pahadi_rajma",
        name = "Harsil Valley Pahadi Rajma",
        category = "Grains & Pulses",
        region = "Garhwal",
        altitude = "2,600m",
        unitLabel = "1 kg Pack",
        priceInr = 340.0,
        description =
          "Thin-skinned, creamy alpine kidney beans prized across Uttarakhand for their quick cooking time and rich earthy broth.",
        harvestNote = "Sun-Dried October Crop",
        isBestSeller = true,
      ),
      OrganicProduct(
        id = "kuttu_buckwheat",
        name = "Stone-Ground Himalayan Buckwheat",
        category = "Grains & Pulses",
        region = "Kinnaur",
        altitude = "2,900m",
        unitLabel = "500g Pouch",
        priceInr = 240.0,
        description =
          "Freshly stone-milled (gharat) organic buckwheat flour packed with rutin, plant protein, and slow-release energy.",
        harvestNote = "Watermill Stone-Ground",
      ),
      OrganicProduct(
        id = "chamomile_tulsi_tea",
        name = "Alpine Chamomile & Rama Tulsi Tea",
        category = "Teas & Herbs",
        region = "Kumaon",
        altitude = "2,000m",
        unitLabel = "75g Tin",
        priceInr = 420.0,
        description =
          "Whole shade-dried mountain chamomile blossoms blended with sacred Rama Tulsi and wild lemongrass for restorative evenings.",
        harvestNote = "Hand-Plucked Whole Flowers",
      ),
      OrganicProduct(
        id = "buransh_squash",
        name = "Garhwal Buransh (Rhododendron) Elixir",
        category = "Oils & Elixirs",
        region = "Garhwal",
        altitude = "2,300m",
        unitLabel = "500ml Bottle",
        priceInr = 490.0,
        description =
          "Vibrant crimson spring nectar crafted from wild Rhododendron arboreum petals and organic jaggery, rich in antioxidants.",
        harvestNote = "Spring Blossom Cold Extraction",
      ),
      OrganicProduct(
        id = "apricot_kernel_oil",
        name = "Cold-Pressed Chuli (Apricot) Oil",
        category = "Oils & Elixirs",
        region = "Kinnaur",
        altitude = "2,750m",
        unitLabel = "250ml Bottle",
        priceInr = 580.0,
        description =
          "Virgin cold-pressed wild Himalayan apricot kernel oil suitable for gourmet dressings as well as nourishing skin care.",
        harvestNote = "Wooden Kolhu Cold-Pressed",
      ),
    )
}

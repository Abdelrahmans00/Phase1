package com.example.phase1.dto;

import java.util.List;

public class CampaignRequest {
    public String organizationEmail;
    public String title;
    public String description;
    public String category;
    public List<CampaignItemDTO> needed_items;
}

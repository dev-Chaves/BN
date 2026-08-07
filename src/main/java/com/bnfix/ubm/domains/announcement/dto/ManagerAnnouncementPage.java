package com.bnfix.ubm.domains.announcement.dto;
import java.util.List;
public record ManagerAnnouncementPage(List<ManagerAnnouncement> items, int page, int size, boolean hasMore) {}

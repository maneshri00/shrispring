package com.productivity_mangement.productivity.service;

import com.productivity_mangement.productivity.DTO.EmailDTO;
import com.productivity_mangement.productivity.entity.UserProfile;
import com.productivity_mangement.productivity.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProfileInferenceService {

    private final GmailService gmailService;
    private final UserProfileRepository userProfileRepository;

    public ProfileInferenceService(GmailService gmailService,
                                   UserProfileRepository userProfileRepository) {
        this.gmailService = gmailService;
        this.userProfileRepository = userProfileRepository;
    }

    public void inferAndApply(String email) {
        try {
            List<EmailDTO> emails = gmailService.fetchRecentEmails(30);
            InferenceResult result = infer(emails);
            apply(email, result);
        } catch (IOException | IllegalStateException e) {
            // swallow errors; don't block login
        }
    }

    private void apply(String email, InferenceResult result) {
        UserProfile profile = userProfileRepository.findByEmail(email).orElseGet(UserProfile::new);
        profile.setEmail(email);
        if (result.profession != null && !result.profession.isBlank()) {
            profile.setProfession(result.profession);
        }
        if (result.priorities != null && !result.priorities.isEmpty()) {
            List<String> existing = profile.getPriorities();
            Set<String> merged = new LinkedHashSet<>();
            if (existing != null) merged.addAll(existing);
            merged.addAll(result.priorities);
            profile.setPriorities(new ArrayList<>(merged));
        }
        userProfileRepository.save(profile);
    }

    private InferenceResult infer(List<EmailDTO> emails) {
        Map<String, Integer> domainCounts = new HashMap<>();
        Map<String, Integer> keywordCounts = new HashMap<>();

        Pattern emailPattern = Pattern.compile("<([^>]+)>|\\b[\\w._%+-]+@([\\w.-]+)\\.[A-Za-z]{2,}\\b");

        for (EmailDTO e : emails) {
            String from = Optional.ofNullable(e.getFrom()).orElse("");
            String subject = Optional.ofNullable(e.getSubject()).orElse("").toLowerCase(Locale.ROOT);
            String snippet = Optional.ofNullable(e.getSnippet()).orElse("").toLowerCase(Locale.ROOT);

            String domain = extractDomain(from, emailPattern);
            if (domain != null) {
                domain = domain.toLowerCase(Locale.ROOT);
                domainCounts.put(domain, domainCounts.getOrDefault(domain, 0) + 1);
            }

            countKeywords(subject, keywordCounts);
            countKeywords(snippet, keywordCounts);
        }

        String profession = inferProfession(domainCounts, keywordCounts);
        List<String> priorities = inferPriorities(keywordCounts);
        return new InferenceResult(profession, priorities);
    }

    private String extractDomain(String from, Pattern pattern) {
        Matcher m = pattern.matcher(from);
        if (m.find()) {
            String match = m.group(1) != null ? m.group(1) : m.group(0);
            int at = match.indexOf('@');
            if (at >= 0) {
                return match.substring(at + 1).trim();
            }
        }
        return null;
    }

    private void countKeywords(String text, Map<String, Integer> counts) {
        String[] keys = new String[]{
                "jira","github","deploy","build","bug","ticket","issue","merge","pull request","commit","slack",
                "meeting","schedule","calendar","deadline","due","report","status",
                "design","figma","mockup","prototype",
                "study","assignment","exam","course","university","college",
                "marketing","campaign","ad","branding","seo"
        };
        for (String k : keys) {
            if (text.contains(k)) {
                counts.put(k, counts.getOrDefault(k, 0) + 1);
            }
        }
    }

    private String inferProfession(Map<String, Integer> domains, Map<String, Integer> keywords) {
        int engineerScore = score(domains, List.of("github.com","gitlab.com","atlassian.net","slack.com","notion.so"))
                + scoreKeywords(keywords, List.of("jira","github","deploy","bug","ticket","issue","merge","pull request","commit","slack"));
        int designerScore = scoreKeywords(keywords, List.of("design","figma","mockup","prototype"));
        int studentScore = scoreKeywords(keywords, List.of("assignment","exam","course","university","college","study"));
        int marketerScore = scoreKeywords(keywords, List.of("marketing","campaign","ad","branding","seo"));
        int managerScore = scoreKeywords(keywords, List.of("meeting","report","status","deadline","schedule"));

        int max = Math.max(engineerScore, Math.max(designerScore, Math.max(studentScore, Math.max(marketerScore, managerScore))));
        if (max == 0) return "general";
        if (max == engineerScore) return "software engineer";
        if (max == designerScore) return "designer";
        if (max == studentScore) return "student";
        if (max == marketerScore) return "marketer";
        return "manager";
    }

    private int score(Map<String, Integer> map, List<String> keys) {
        int s = 0;
        for (String k : keys) s += map.getOrDefault(k, 0);
        return s;
    }

    private int scoreKeywords(Map<String, Integer> map, List<String> keys) {
        int s = 0;
        for (String k : keys) s += map.getOrDefault(k, 0);
        return s;
    }

    private List<String> inferPriorities(Map<String, Integer> keywords) {
        Map<String, Integer> areas = new LinkedHashMap<>();
        areas.put("coding", sum(keywords, List.of("github","jira","bug","ticket","issue","merge","pull request","commit","deploy")));
        areas.put("meetings", sum(keywords, List.of("meeting","schedule","calendar","status","report")));
        areas.put("design", sum(keywords, List.of("design","figma","mockup","prototype")));
        areas.put("learning", sum(keywords, List.of("study","assignment","exam","course","university","college")));
        areas.put("marketing", sum(keywords, List.of("marketing","campaign","branding","seo","ad")));
        areas.put("deadlines", sum(keywords, List.of("deadline","due")));

        List<Map.Entry<String, Integer>> list = new ArrayList<>(areas.entrySet());
        list.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : list) {
            if (e.getValue() > 0) result.add(e.getKey());
            if (result.size() >= 3) break;
        }
        if (result.isEmpty()) result.add("general");
        return result;
    }

    private int sum(Map<String, Integer> map, List<String> keys) {
        int s = 0;
        for (String k : keys) s += map.getOrDefault(k, 0);
        return s;
    }

    static class InferenceResult {
        final String profession;
        final List<String> priorities;
        InferenceResult(String profession, List<String> priorities) {
            this.profession = profession;
            this.priorities = priorities;
        }
    }
}


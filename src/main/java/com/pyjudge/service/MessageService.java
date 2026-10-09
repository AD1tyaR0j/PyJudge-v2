package com.pyjudge.service;

import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.UserDao;
import com.pyjudge.model.Message;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Messaging between students and staff. */
public class MessageService {
    public static final int MAX_LENGTH = 4000;

    /** One row of the inbox: the other person, the latest message and how many are unread. */
    public static final class Conversation {
        private final User user;
        private final Message last;
        private int unread;

        Conversation(User user, Message last) {
            this.user = user;
            this.last = last;
        }

        public User getUser() { return user; }
        public Message getLast() { return last; }
        public int getUnread() { return unread; }
    }

    private final MessageDao messages;
    private final UserDao users;
    private final SubmissionDao submissions;
    private final SettingsService settings;

    public MessageService(MessageDao messages, UserDao users, SubmissionDao submissions, SettingsService settings) {
        this.messages = messages;
        this.users = users;
        this.submissions = submissions;
        this.settings = settings;
    }

    /** Students talk to instructors and admins; instructors and admins can message anyone. */
    public boolean canMessage(User sender, User recipient) {
        if (recipient == null || recipient.getId() == sender.getId() || recipient.isBlocked()) {
            return false;
        }
        if (!settings.flag("messaging_enabled") && !sender.isAdmin()) {
            return false;
        }
        return !sender.isStudent() || recipient.isAuthor();
    }

    /** Everyone the user is allowed to start a conversation with. */
    public List<User> contacts(User user) {
        return users.listActiveExcept(user.getId()).stream()
                .filter(other -> canMessage(user, other))
                .collect(Collectors.toList());
    }

    /** The user's conversations, most recent first. */
    public List<Conversation> conversations(User user) {
        Map<Integer, Conversation> byOther = new LinkedHashMap<>();
        for (Message m : messages.listInvolving(user.getId())) { // newest first, so the first one seen is "last"
            User other = m.getSenderId() == user.getId() ? m.getRecipient() : m.getSender();
            Conversation conversation = byOther.computeIfAbsent(other.getId(), id -> new Conversation(other, m));
            if (m.getRecipientId() == user.getId() && m.isUnread()) {
                conversation.unread++;
            }
        }
        return new ArrayList<>(byOther.values());
    }

    /**
     * Sends a message.
     *
     * @param recipient    may be null (nobody picked)
     * @param submissionId an optional submission to attach; ignored unless the sender may view it
     */
    public Outcome send(User sender, User recipient, String body, Integer submissionId) {
        if (!canMessage(sender, recipient)) {
            return Outcome.fail(recipient == null ? "Pick someone to message." : "You can't message that person.");
        }
        String text = body == null ? "" : body.strip();
        if (text.isEmpty()) {
            return Outcome.fail("Write a message first.");
        }
        Integer attach = null;
        if (submissionId != null) {
            attach = submissions.findById(submissionId).filter(s -> s.viewableBy(sender)).map(Submission::getId).orElse(null);
        }
        messages.insert(sender.getId(), recipient.getId(), text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text,
                attach);
        return Outcome.ok("Message sent to " + recipient.getDisplayName() + ".");
    }
}

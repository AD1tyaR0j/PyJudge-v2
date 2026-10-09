package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.Message;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MessageDao extends BaseDao {
    private static final String SELECT =
            "SELECT m.*, " + userColumns("s", "from") + ", " + userColumns("r", "to")
                    + ", sp.title AS sub_problem_title"
                    + " FROM messages m"
                    + " JOIN users s ON s.id = m.sender_id"
                    + " JOIN users r ON r.id = m.recipient_id"
                    + " LEFT JOIN submissions sub ON sub.id = m.submission_id"
                    + " LEFT JOIN problems sp ON sp.id = sub.problem_id";

    public MessageDao(Database db) {
        super(db);
    }

    private static Message map(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setSenderId(rs.getInt("sender_id"));
        m.setRecipientId(rs.getInt("recipient_id"));
        m.setBody(rs.getString("body"));
        m.setSubmissionId(intOrNull(rs, "submission_id"));
        m.setCreatedAt(timestamp(rs, "created_at"));
        m.setReadAt(timestamp(rs, "read_at"));
        m.setSender(joinedUser(rs, "from"));
        m.setRecipient(joinedUser(rs, "to"));
        if (m.getSubmissionId() != null) {
            Problem problem = new Problem();
            problem.setTitle(rs.getString("sub_problem_title"));
            Submission submission = new Submission();
            submission.setId(m.getSubmissionId());
            submission.setProblem(problem);
            m.setSubmission(submission);
        }
        return m;
    }

    public Message insert(int senderId, int recipientId, String body, Integer submissionId) {
        Message m = new Message();
        m.setSenderId(senderId);
        m.setRecipientId(recipientId);
        m.setBody(body);
        m.setSubmissionId(submissionId);
        m.setCreatedAt(Times.now());
        m.setId(db.insert("INSERT INTO messages (sender_id, recipient_id, body, submission_id, created_at)"
                + " VALUES (?, ?, ?, ?, ?)", senderId, recipientId, body, submissionId, m.getCreatedAt()));
        return m;
    }

    public int countUnread(int userId) {
        return (int) db.count("SELECT COUNT(*) FROM messages WHERE recipient_id = ? AND read_at IS NULL", userId);
    }

    /** Everything the user sent or received, newest first. */
    public List<Message> listInvolving(int userId) {
        return db.query(SELECT + " WHERE m.sender_id = ? OR m.recipient_id = ? ORDER BY m.id DESC",
                MessageDao::map, userId, userId);
    }

    /** The newest messages the user received. */
    public List<Message> recentReceived(int userId, int limit) {
        return db.query(SELECT + " WHERE m.recipient_id = ? ORDER BY m.id DESC LIMIT ?", MessageDao::map, userId, limit);
    }

    /** The conversation between two users, oldest first. */
    public List<Message> thread(int userA, int userB) {
        return db.query(SELECT + " WHERE (m.sender_id = ? AND m.recipient_id = ?)"
                        + " OR (m.sender_id = ? AND m.recipient_id = ?) ORDER BY m.id",
                MessageDao::map, userA, userB, userB, userA);
    }

    /** Marks everything {@code senderId} sent to {@code recipientId} as read. */
    public void markRead(int recipientId, int senderId) {
        db.update("UPDATE messages SET read_at = ? WHERE recipient_id = ? AND sender_id = ? AND read_at IS NULL",
                Times.now(), recipientId, senderId);
    }
}

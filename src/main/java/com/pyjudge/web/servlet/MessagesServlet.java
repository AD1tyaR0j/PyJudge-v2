package com.pyjudge.web.servlet;

import com.pyjudge.model.Message;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.service.Outcome;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;
import java.util.List;

/** Messaging between students and staff: the inbox, starting a conversation, and a conversation thread. */
@WebServlet(name = "messages", urlPatterns = {"/messages", "/messages/*"})
public class MessagesServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/messages", this::inbox);
        get("/messages/new", this::composeForm); // registered before /messages/{username}
        post("/messages/new", this::compose);
        get("/messages/{username}", this::thread);
        post("/messages/{username}", this::reply);
    }

    private void inbox(Ctx ctx) throws Exception {
        ctx.set("conversations", app.messaging().conversations(ctx.requireUser()));
        ctx.render("messages/inbox");
    }

    private void composeForm(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        Integer submissionId = ctx.intParam("submission");
        Submission submission = submissionId == null ? null
                : app.submissions().findById(submissionId).filter(s -> s.viewableBy(user)).orElse(null);
        ctx.set("contacts", app.messaging().contacts(user)).set("to", ctx.param("to")).set("submission", submission);
        ctx.render("messages/compose");
    }

    private void compose(Ctx ctx) throws Exception {
        send(ctx, app.users().findByUsername(ctx.param("to")).orElse(null));
    }

    private void thread(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        User other = app.users().findByUsername(ctx.path("username")).orElseThrow(HttpError::notFound);
        List<Message> messages = app.messages().thread(user.getId(), other.getId());
        boolean canReply = app.messaging().canMessage(user, other);
        if (messages.isEmpty() && !canReply) {
            throw HttpError.notFound(); // no history and no right to start one: nothing to show
        }
        app.messages().markRead(user.getId(), other.getId());
        ctx.set("other", other).set("msgs", messages).set("canReply", canReply);
        ctx.render("messages/thread");
    }

    private void reply(Ctx ctx) throws Exception {
        send(ctx, app.users().findByUsername(ctx.path("username")).orElseThrow(HttpError::notFound));
    }

    private void send(Ctx ctx, User recipient) throws Exception {
        Outcome outcome = app.messaging().send(ctx.requireUser(), recipient, ctx.param("body"), ctx.intParam("submission_id"));
        if (ctx.flash(outcome, "success")) {
            ctx.redirect("/messages/" + recipient.getUsername());
        } else {
            ctx.redirectBack("/messages");
        }
    }
}

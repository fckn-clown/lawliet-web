package xyz.lawlietbot.spring.frontend.components.premium;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.QueryParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.lawlietbot.spring.backend.Redirector;
import xyz.lawlietbot.spring.backend.payment.Subscription;
import xyz.lawlietbot.spring.backend.payment.paddle.PaddleAPI;
import xyz.lawlietbot.spring.backend.payment.paddle.PaddleManager;
import xyz.lawlietbot.spring.backend.subscriptionfeedback.SubscriptionFeedbackIdManager;
import xyz.lawlietbot.spring.backend.userdata.DiscordUser;
import xyz.lawlietbot.spring.backend.userdata.SessionData;
import xyz.lawlietbot.spring.frontend.components.ConfirmationDialog;
import xyz.lawlietbot.spring.frontend.components.CustomNotification;
import xyz.lawlietbot.spring.syncserver.SyncUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PremiumManagePage extends PremiumPage {

    private final static Logger LOGGER = LoggerFactory.getLogger(PremiumManagePage.class);

    private final SessionData sessionData;
    private final ConfirmationDialog dialog;
    private final PremiumUnlockPage premiumUnlockPage;

    public PremiumManagePage(SessionData sessionData, ConfirmationDialog dialog, PremiumUnlockPage premiumUnlockPage) {
        this.sessionData = sessionData;
        this.dialog = dialog;
        this.premiumUnlockPage = premiumUnlockPage;

        setPadding(true);
    }

    @Override
    public void build() {
        sessionData.getDiscordUser().ifPresent(user -> updateMainContent(user, 0));
    }

    private void updateMainContent(DiscordUser user, long reloadSubId) {
        removeAll();
        Component grid = generateGrid(user, reloadSubId);
        add(grid);
    }

    private Component generateGrid(DiscordUser user, long reloadSubId) {
        List<Subscription> subscriptionList = SyncUtil.retrievePaddleSubscriptions(user.getId(), reloadSubId).join();
        if (!subscriptionList.isEmpty()) {
            Grid<Subscription> grid = new Grid<>(Subscription.class, false);
            grid.setHeightByRows(true);
            grid.setItems(subscriptionList);
            grid.setSelectionMode(Grid.SelectionMode.NONE);

            grid.addColumn(sub -> getTranslation("premium.tier." + PaddleManager.getSubLevelType(sub.getPlanId()).name()))
                    .setHeader(getTranslation("manage.grid.header.level"))
                    .setAutoWidth(true);
            grid.addColumn(sub -> getTranslation("manage.grid.status", sub.isActive()))
                    .setHeader(getTranslation("manage.grid.header.status"))
                    .setAutoWidth(true);
            grid.addColumn(Subscription::getQuantity)
                    .setHeader(getTranslation("manage.grid.header.quantity"))
                    .setAutoWidth(true);
            grid.addColumn(Subscription::getPrice)
                    .setHeader(getTranslation("manage.grid.header.price"))
                    .setAutoWidth(true);
            grid.addColumn(Subscription::getNextPayment)
                    .setHeader(getTranslation("manage.grid.header.nextpayment"))
                    .setAutoWidth(true);
            grid.addComponentColumn(sub -> generateActionComponent(sub, user))
                    .setAutoWidth(true);
            return grid;
        } else {
            Div textDiv = new Div(new Text(getTranslation("manage.nosubs")));
            textDiv.setWidthFull();
            textDiv.getStyle().set("text-align", "center");
            return textDiv;
        }
    }

    private Component generateActionComponent(Subscription sub, DiscordUser user) {
        List<String> actionList = sub.isActive()
                ? List.of("pause", "payment_details")
                : List.of("resume", "cancel", "payment_details");

        Select<String> actionSelect = new Select<>();
        actionSelect.setPlaceholder(getTranslation("manage.grid.action"));
        actionSelect.setWidthFull();
        actionSelect.setItems(actionList);
        actionSelect.setTextRenderer(action -> getTranslation("manage.grid.action." + action));
        actionSelect.addValueChangeListener(e -> {
            String action = e.getValue();
            if (action != null) {
                if (action.equals("payment_details")) {
                    new Redirector().redirect(sub.getUpdateUrl());
                } else {
                    Span outerSpan = new Span(getTranslation("manage.grid.action.dialog." + action));
                    outerSpan.setWidthFull();
                    outerSpan.getStyle().set("color", "black");
                    if (action.equals("cancel")) {
                        Span innerSpan = new Span(" " + getTranslation("manage.grid.action.dialog.cancel.warning"));
                        innerSpan.getStyle().set("color", "var(--lumo-error-text-color)");
                        outerSpan.add(innerSpan);
                    }

                    dialog.open(outerSpan, () -> {
                        boolean success = false;
                        boolean navigateToFeedbackPage = false;
                        try {
                            switch (action) {
                                case "pause":
                                    success = PaddleAPI.subscriptionSetPaused(sub.getSubId(), true);
                                    navigateToFeedbackPage = true;
                                    break;

                                case "resume":
                                    success = PaddleAPI.subscriptionSetPaused(sub.getSubId(), false);
                                    break;

                                case "cancel":
                                    success = PaddleAPI.subscriptionCancel(sub.getSubId());
                                    break;

                                default:
                            }
                        } catch (IOException ioException) {
                            LOGGER.error("Exception on sub update", ioException);
                        }
                        if (success) {
                            updateMainContent(user, sub.getSubId());
                            CustomNotification.showSuccess(getTranslation("manage.success"));
                            if (navigateToFeedbackPage) {
                                QueryParameters queryParameters = new QueryParameters(Map.of("id", List.of(SubscriptionFeedbackIdManager.generateId())));
                                UI.getCurrent().navigate("/subscriptionfeedback", queryParameters);
                            } else {
                                premiumUnlockPage.update();
                            }
                        } else {
                            CustomNotification.showError(getTranslation("error"));
                        }
                    }, () -> actionSelect.setValue(null));
                }
            }
        });

        return actionSelect;
    }
}

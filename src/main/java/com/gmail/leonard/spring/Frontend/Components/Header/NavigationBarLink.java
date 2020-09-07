package com.gmail.leonard.spring.Frontend.Components.Header;

import com.gmail.leonard.spring.Backend.Language.PageTitleGen;
import com.gmail.leonard.spring.Frontend.Layouts.PageLayout;
import com.gmail.leonard.spring.NoLiteAccess;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.RouterLink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class NavigationBarLink {

    private Anchor anchor;
    private RouterLink routerLink;
    private final boolean hiddenInLiteVersion;

    public NavigationBarLink(String externalLink, String id) {
        this(externalLink, id, false);
    }

    public NavigationBarLink(String externalLink, String id, boolean hiddenInLiteVersion) {
        anchor = new Anchor(externalLink, new Text(PageTitleGen.getTitle(id)));
        anchor.setTarget("_blank");
        anchor.setWidthFull();

        this.hiddenInLiteVersion = hiddenInLiteVersion;
    }

    public NavigationBarLink(Class<? extends PageLayout> page) {
        this.routerLink = new RouterLink(PageTitleGen.getTitle(PageLayout.getRouteStatic(page)), page);
        hiddenInLiteVersion = page.isAnnotationPresent(NoLiteAccess.class);
    }

    public NavigationBarLink standOut() {
        Style style;
        if (anchor != null) style = anchor.getStyle();
        else style = routerLink.getStyle();

        style.set("text-shadow", "0 0 10px var(--lumo-primary-color)");
        return this;
    }

    public Optional<Anchor> getAnchor() {
        return Optional.ofNullable(anchor);
    }

    public Optional<RouterLink> getRouterLink() {
        return Optional.ofNullable(routerLink);
    }

    public String getLabel() {
        if (getRouterLink().isPresent()) return getRouterLink().get().getText();
        else if (getAnchor().isPresent()) return getAnchor().get().getText();
        return "";
    }

    public boolean isHiddenInLiteVersion() {
        return hiddenInLiteVersion;
    }
}

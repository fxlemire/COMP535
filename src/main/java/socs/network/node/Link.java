package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;

  public Link(RouterDescription r1, RouterDescription r2) {
    router1 = r1;
    router2 = r2;
  }

  public RouterDescription getRouter1() {
    return router1;
  }

  public RouterDescription getRouter2() {
    return router2;
  }
}

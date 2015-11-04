package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;
  short _weight;

  public Link(RouterDescription r1, RouterDescription r2, short weight) {
    router1 = r1;
    router2 = r2;
    _weight = weight;
  }

  public RouterDescription getRouter1() {
    return router1;
  }

  public RouterDescription getRouter2() {
    return router2;
  }

  public short getWeight() {
    return _weight;
  }
}

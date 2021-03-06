// Sewerin Kuss 201346
// Duc Anh Le 230662
// Janis Melon 209928

package com.example;

import akka.actor.typed.ActorSystem;

import java.io.IOException;
public class AkkaCoffeeStart {
  public static void main(String[] args) {
    //#actor-system
    final ActorSystem<CoffeeMain.StartMessage> coffeeMain = ActorSystem.create(CoffeeMain.create(), "mainCoffeeActor");
    //#actor-system

    //#main-send-messages
    coffeeMain.tell(new CoffeeMain.StartMessage());
    //#main-send-messages

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
      coffeeMain.terminate();
    }
  }
}

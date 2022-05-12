package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class LoadBalancer extends AbstractBehavior<LoadBalancer.Mixed> {

    private ActorRef<CoffeeMachine.Request>[] coffeeMachinesList = null;

    public interface Mixed {
    }

    // TODO: should return the coffee machine with the highest amount of remaining coffee
    public static final class CoffeeSuccess implements Mixed {
        public ActorRef<Customer.Response> sender;

        public CoffeeSuccess(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    // after cash register confirmed that the customer has enough money
    public static class CreditSuccess implements Mixed {
        public ActorRef<CashRegister.Request> sender;

        public CreditSuccess(ActorRef<CashRegister.Request> sender) {
            this.sender = sender;
        }
    }

    // after cash register confirmed that the customer doesn't have enough money
    public static final class CreditFail implements Mixed {
        public ActorRef<LoadBalancer.Mixed> sender;

        public CreditFail(ActorRef<LoadBalancer.Mixed> sender) {
            this.sender = sender;
        }
    }

    // after receiving the supply report from a coffee machine
    public static final class GetSupply implements Mixed {
        public ActorRef<CoffeeMachine.Request> sender;
        public ActorRef<CoffeeMachine.Request> coffeeMachineMax;
        public int max;

        public GetSupply(ActorRef<CoffeeMachine.Request> sender, int remainingCoffee) {
            this.sender = sender;
            if (remainingCoffee > max) {
                max = remainingCoffee;
                coffeeMachineMax = sender;
            }
        }
    }

    // customer asks load balancer for a coffee
    public static final class GetCoffee implements Mixed {
        public ActorRef<Customer.Response> sender;

        public GetCoffee(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    public static Behavior<LoadBalancer.Mixed> create(ActorRef<CoffeeMachine.Request>[] coffeeMachinesList) {
        return Behaviors.setup(context -> new LoadBalancer(context, coffeeMachinesList));
    }

    public LoadBalancer(ActorContext<Mixed> context, ActorRef<CoffeeMachine.Request>[] coffeeMachinesList) {
        super(context);
        this.coffeeMachinesList = coffeeMachinesList;
    }

    @Override
    public Receive<Mixed> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreditSuccess.class, this::onCreditSuccess)
                .onMessage(CreditFail.class, this::onCreditFail)
                .onMessage(CoffeeSuccess.class, this::onCoffeeSuccess)
                .onMessage(GetSupply.class, this::onGetSupply)
                .onMessage(GetCoffee.class, this::onGetCoffee)
                .build();
    }

    //the customer has enough money for a coffee
    private Behavior<Mixed> onCreditSuccess(CreditSuccess respond) {
        //then the load balancer asks all the coffee machines for their supplies
        this.getContext().getSelf().tell(new CoffeeMachine.GiveSupply(this.getContext().getSelf()));
        return this;
    }

    //the customer doesn't have enough money for a coffee
    private Behavior<Mixed> onCreditFail(CreditFail respond) {
        this.getContext().getSelf().tell(new Customer.Fail());
        return this;
    }

    // TODO: should return the coffee machine with the highest amount of remaining coffee
    private Behavior<Mixed> onCoffeeSuccess(CreditFail respond) {
        this.getContext().getSelf().tell(new Customer.GetCoffeeMachine(this.getContext().getSelf(),));
        return this;
    }

    // customer asks load balancer for a coffee
    private Behavior<Mixed> onGetCoffee(GetCoffee request) {
        getContext().getLog().info("Got a get request from {}!", request.sender.path());
        //load balancer asks cash register if he/she has enough money for a coffee
        this.getContext().getSelf().tell(new CashRegister.State((this.getContext().getSelf()));
        return this;
    }

    // returns the coffee machine with the most coffee to the customer
    private Behavior<Mixed> onGetSupply(GetSupply response) {
        getContext().getLog().info("Got a supply request from {}!", response.sender.path());
        for (ActorRef<CoffeeMachine.Request> coffeeMachine :
                coffeeMachinesList) {
            coffeeMachine.tell(new CoffeeMachine.GiveSupply(this.getContext().getSelf()));
        }
        return this;
    }
}

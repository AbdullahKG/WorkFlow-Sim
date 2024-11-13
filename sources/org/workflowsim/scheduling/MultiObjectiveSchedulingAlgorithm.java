package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.WorkflowSimTags;

public class MultiObjectiveSchedulingAlgorithm extends BaseSchedulingAlgorithm {
    
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 100;
    private final Random random = new Random();

    @Override
    public void run() {
        List<List<Integer>> population = initializePopulation(POPULATION_SIZE, getCloudletList().size());
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            population = evolvePopulation(population);
        }
        List<Integer> bestSolution = getBestSolution(population);
        scheduleTasks(bestSolution);
    }

    private List<List<Integer>> initializePopulation(int populationSize, int taskSize) {
        List<List<Integer>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Integer> individual = new ArrayList<>();
            for (int j = 0; j < taskSize; j++) {
                individual.add(random.nextInt(getVmList().size()));
            }
            population.add(individual);
        }
        return population;
    }

    private List<List<Integer>> evolvePopulation(List<List<Integer>> population) {
        List<List<Integer>> newPopulation = new ArrayList<>();
        for (int i = 0; i < population.size(); i++) {
            List<Integer> parent1 = selectIndividual(population);
            List<Integer> parent2 = selectIndividual(population);
            List<Integer> child = crossover(parent1, parent2);
            mutate(child);
            newPopulation.add(child);
        }
        return newPopulation;
    }

    private List<Integer> selectIndividual(List<List<Integer>> population) {
        List<Integer> best = null;
        for (int i = 0; i < 5; i++) {  // Tournament selection
            List<Integer> individual = population.get(random.nextInt(population.size()));
            if (best == null || dominance(individual, best)) {
                best = individual;
            }
        }
        return best;
    }

    private List<Integer> crossover(List<Integer> parent1, List<Integer> parent2) {
        List<Integer> child = new ArrayList<>();
        for (int i = 0; i < parent1.size(); i++) {
            if (random.nextBoolean()) {
                child.add(parent1.get(i));
            } else {
                child.add(parent2.get(i));
            }
        }
        return child;
    }

    private void mutate(List<Integer> individual) {
        for (int i = 0; i < individual.size(); i++) {
            if (random.nextDouble() < 0.01) {  // Mutation rate
                individual.set(i, random.nextInt(getVmList().size()));
            }
        }
    }

    private boolean dominance(List<Integer> individual1, List<Integer> individual2) {
        double[] fitness1 = evaluateFitness(individual1);
        double[] fitness2 = evaluateFitness(individual2);
        boolean betterInAtLeastOne = false;
        for (int i = 0; i < fitness1.length; i++) {
            if (fitness1[i] < fitness2[i]) {
                betterInAtLeastOne = true;
            } else if (fitness1[i] > fitness2[i]) {
                return false;
            }
        }
        return betterInAtLeastOne;
    }

    private double[] evaluateFitness(List<Integer> individual) {
        double makespan = 0.0;
        double cost = 0.0;
        int[] vmLoad = new int[getVmList().size()];
        for (int i = 0; i < individual.size(); i++) {
            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(i);
            CondorVM vm = (CondorVM) getVmList().get(individual.get(i));
            double time = cloudlet.getCloudletLength() / vm.getCurrentRequestedTotalMips();
            double vmCost = time * vm.getCost();
            makespan = Math.max(makespan, vmLoad[individual.get(i)] + time);
            vmLoad[individual.get(i)] += time;
            cost += vmCost;
        }
        return new double[]{makespan, cost};
    }

    private List<Integer> getBestSolution(List<List<Integer>> population) {
        List<Integer> best = null;
        for (List<Integer> individual : population) {
            if (best == null || dominance(individual, best)) {
                best = individual;
            }
        }
        return best;
    }

    private void scheduleTasks(List<Integer> solution) {
        for (int i = 0; i < solution.size(); i++) {
            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(i);
            CondorVM vm = (CondorVM) getVmList().get(solution.get(i));
            cloudlet.setVmId(vm.getId());
            getScheduledList().add(cloudlet);
        }
    }
}


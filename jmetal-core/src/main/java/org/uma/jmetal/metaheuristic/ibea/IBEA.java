//  IBEA.java
//
//  Author:
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.uma.jmetal.metaheuristic.ibea;

import org.uma.jmetal.core.*;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.Ranking;
import org.uma.jmetal.util.comparator.DominanceComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implementing the IBEA algorithm
 */
public class IBEA extends Algorithm {

  /**
   * Defines the number of tournaments for creating the mating pool
   */
  public static final int TOURNAMENTS_ROUNDS = 1;
  /**
   *
   */
  private static final long serialVersionUID = -1889165434725718813L;
  /**
   * Stores the value of the indicator between each pair of solutions into
   * the solutiontype set
   */
  private List<List<Double>> indicatorValues_;

  /**
   *
   */
  private double maxIndicatorValue_;

  /**
   * Constructor.
   * Create a new IBEA instance
   *
   * @param problem Problem to solve
   */
  public IBEA() {
    super();
  }

  /**
   * calculates the hypervolume of that portion of the objective space that
   * is dominated by individual a but not by individual b
   */
  double calcHypervolumeIndicator(Solution p_ind_a,
    Solution p_ind_b,
    int d,
    double maximumValues[],
    double minimumValues[]) {
    double a, b, r, max;
    double volume = 0;
    double rho = 2.0;

    r = rho * (maximumValues[d - 1] - minimumValues[d - 1]);
    max = minimumValues[d - 1] + r;


    a = p_ind_a.getObjective(d - 1);
    if (p_ind_b == null) {
      b = max;
    } else {
      b = p_ind_b.getObjective(d - 1);
    }

    if (d == 1) {
      if (a < b) {
        volume = (b - a) / r;
      } else {
        volume = 0;
      }
    } else {
      if (a < b) {
        volume = calcHypervolumeIndicator(p_ind_a, null, d - 1, maximumValues, minimumValues) *
          (b - a) / r;
        volume += calcHypervolumeIndicator(p_ind_a, p_ind_b, d - 1, maximumValues, minimumValues) *
          (max - b) / r;
      } else {
        volume = calcHypervolumeIndicator(p_ind_a, p_ind_b, d - 1, maximumValues, minimumValues) *
          (max - b) / r;
      }
    }

    return (volume);
  }

  /**
   * This structure store the indicator values of each pair of elements
   */
  public void computeIndicatorValuesHD(SolutionSet solutionSet,
    double[] maximumValues,
    double[] minimumValues) throws JMetalException {
    SolutionSet A, B;
    // Initialize the structures
    indicatorValues_ = new ArrayList<List<Double>>();
    maxIndicatorValue_ = -Double.MAX_VALUE;

    for (int j = 0; j < solutionSet.size(); j++) {
      A = new SolutionSet(1);
      A.add(solutionSet.get(j));

      List<Double> aux = new ArrayList<Double>();
      for (int i = 0; i < solutionSet.size(); i++) {
        B = new SolutionSet(1);
        B.add(solutionSet.get(i));

        int flag = (new DominanceComparator()).compare(A.get(0), B.get(0));

        double value = 0.0;
        if (flag == -1) {
          value = -calcHypervolumeIndicator(A.get(0), B.get(0), problem_.getNumberOfObjectives(),
            maximumValues, minimumValues);
        } else {
          value = calcHypervolumeIndicator(B.get(0), A.get(0), problem_.getNumberOfObjectives(),
            maximumValues, minimumValues);
        }

        //Update the max value of the indicator
        if (Math.abs(value) > maxIndicatorValue_) {
          maxIndicatorValue_ = Math.abs(value);
        }
        aux.add(value);
      }
      indicatorValues_.add(aux);
    }
  }

  /**
   * Calculate the fitness for the individual at position pos
   */
  public void fitness(SolutionSet solutionSet, int pos) {
    double fitness = 0.0;
    double kappa = 0.05;

    for (int i = 0; i < solutionSet.size(); i++) {
      if (i != pos) {
        fitness += Math.exp((-1 * indicatorValues_.get(i).get(pos) / maxIndicatorValue_) / kappa);
      }
    }
    solutionSet.get(pos).setFitness(fitness);
  }

  /**
   * Calculate the fitness for the entire population.
   */
  public void calculateFitness(SolutionSet solutionSet) throws JMetalException {
    // Obtains the lower and upper bounds of the population
    double[] maximumValues = new double[problem_.getNumberOfObjectives()];
    double[] minimumValues = new double[problem_.getNumberOfObjectives()];

    for (int i = 0; i < problem_.getNumberOfObjectives(); i++) {
      maximumValues[i] = -Double.MAX_VALUE;
      minimumValues[i] = Double.MAX_VALUE;
    }

    for (int pos = 0; pos < solutionSet.size(); pos++) {
      for (int obj = 0; obj < problem_.getNumberOfObjectives(); obj++) {
        double value = solutionSet.get(pos).getObjective(obj);
        if (value > maximumValues[obj]) {
          maximumValues[obj] = value;
        }
        if (value < minimumValues[obj]) {
          minimumValues[obj] = value;
        }
      }
    }

    computeIndicatorValuesHD(solutionSet, maximumValues, minimumValues);
    for (int pos = 0; pos < solutionSet.size(); pos++) {
      fitness(solutionSet, pos);
    }
  }

  /**
   * Update the fitness before removing an individual
   */
  public void removeWorst(SolutionSet solutionSet) {

    // Find the worst;
    double worst = solutionSet.get(0).getFitness();
    int worstIndex = 0;
    double kappa = 0.05;

    for (int i = 1; i < solutionSet.size(); i++) {
      if (solutionSet.get(i).getFitness() > worst) {
        worst = solutionSet.get(i).getFitness();
        worstIndex = i;
      }
    }

    // Update the population
    for (int i = 0; i < solutionSet.size(); i++) {
      if (i != worstIndex) {
        double fitness = solutionSet.get(i).getFitness();
        fitness -=
          Math.exp((-indicatorValues_.get(worstIndex).get(i) / maxIndicatorValue_) / kappa);
        solutionSet.get(i).setFitness(fitness);
      }
    }

    // remove worst from the indicatorValues list
    indicatorValues_.remove(worstIndex);
    for (List<Double> anIndicatorValues_ : indicatorValues_) {
      anIndicatorValues_.remove(worstIndex);
    }


    solutionSet.remove(worstIndex);
  }

  /**
   * Runs of the IBEA algorithm.
   *
   * @return aSolutionSet that is a set of non dominated solutions
   * as a result of the algorithm execution
   * @throws org.uma.jmetal.util.JMetalException
   */
  public SolutionSet execute() throws JMetalException, ClassNotFoundException {
    int populationSize, archiveSize, maxEvaluations, evaluations;
    Operator crossoverOperator, mutationOperator, selectionOperator;
    SolutionSet solutionSet, archive, offSpringSolutionSet;

    //Read the params
    populationSize = ((Integer) getInputParameter("populationSize")).intValue();
    archiveSize = ((Integer) getInputParameter("archiveSize")).intValue();
    maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();

    //Read the operator
    crossoverOperator = operators_.get("crossover");
    mutationOperator = operators_.get("mutation");
    selectionOperator = operators_.get("selection");

    //Initialize the variables
    solutionSet = new SolutionSet(populationSize);
    archive = new SolutionSet(archiveSize);
    evaluations = 0;

    //-> Create the initial solutionSet
    Solution newSolution;
    for (int i = 0; i < populationSize; i++) {
      newSolution = new Solution(problem_);
      problem_.evaluate(newSolution);
      problem_.evaluateConstraints(newSolution);
      evaluations++;
      solutionSet.add(newSolution);
    }

    while (evaluations < maxEvaluations) {
      SolutionSet union = ((SolutionSet) solutionSet).union(archive);
      calculateFitness(union);
      archive = union;

      while (archive.size() > populationSize) {
        removeWorst(archive);
      }
      // Create a new offspringPopulation
      offSpringSolutionSet = new SolutionSet(populationSize);
      Solution[] parents = new Solution[2];
      while (offSpringSolutionSet.size() < populationSize) {
        int j = 0;
        do {
          j++;
          parents[0] = (Solution) selectionOperator.execute(archive);
        } while (j < IBEA.TOURNAMENTS_ROUNDS);
        int k = 0;
        do {
          k++;
          parents[1] = (Solution) selectionOperator.execute(archive);
        } while (k < IBEA.TOURNAMENTS_ROUNDS);

        //make the crossover
        Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
        mutationOperator.execute(offSpring[0]);
        problem_.evaluate(offSpring[0]);
        problem_.evaluateConstraints(offSpring[0]);
        offSpringSolutionSet.add(offSpring[0]);
        evaluations++;
      }
      // End Create a offSpring solutionSet
      solutionSet = offSpringSolutionSet;
    }

    Ranking ranking = new Ranking(archive);
    return ranking.getSubfront(0);
  }
}
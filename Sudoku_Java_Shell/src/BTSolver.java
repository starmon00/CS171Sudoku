import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class BTSolver
{

	// =================================================================
	// Properties
	// =================================================================

	private ConstraintNetwork network;
	private SudokuBoard sudokuGrid;
	private Trail trail;

	private boolean hasSolution = false;

	public String varHeuristics;
	public String valHeuristics;
	public String cChecks;

	// =================================================================
	// Constructors
	// =================================================================

	public BTSolver ( SudokuBoard sboard, Trail trail, String val_sh, String var_sh, String cc )
	{
		this.network    = new ConstraintNetwork( sboard );
		this.sudokuGrid = sboard;
		this.trail      = trail;

		varHeuristics = var_sh;
		valHeuristics = val_sh;
		cChecks       = cc;
	}

	// =================================================================
	// Consistency Checks
	// =================================================================

	// Basic consistency check, no propagation done
	private boolean assignmentsCheck ( )
	{
		for ( Constraint c : network.getConstraints() )
			if ( ! c.isConsistent() )
				return false;

		return true;
	}

	/**
	 * Part 1 TODO: Implement the Forward Checking Heuristic
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 * Return: true is assignment is consistent, false otherwise
	 */
	private boolean forwardChecking ( )
	{
		for (Variable var : network.getVariables()) {
			if (var.isAssigned()) {
				for (Variable nei : network.getNeighborsOfVariable(var)) {
					if (var.getAssignment().equals(nei.getAssignment())) {
						return false;
					}
					int assignedVal = var.getAssignment();
					if (nei.getDomain().getValues().contains(assignedVal)) {
						trail.push(nei);
						nei.removeValueFromDomain(assignedVal);
					}
				}
			}
		}

		for (Constraint c : network.getModifiedConstraints()) {
			if (!c.isConsistent()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Part 2 TODO: Implement both of Norvig's Heuristics
	 *
	 * This function will do both Constraint Propagation and check
	 * the consistency of the network
	 *
	 * (1) If a variable is assigned then eliminate that value from
	 *     the square's neighbors.
	 *
	 * (2) If a constraint has only one possible place for a value
	 *     then put the value there.
	 *
	 * Note: remember to trail.push variables before you change their domain
	 * Return: true is assignment is consistent, false otherwise
	 */
	private boolean norvigCheck ( )
	{
		if (!forwardChecking()) {
			return false;
		}
		
		int N = sudokuGrid.getN();

		for (Constraint constraint : network.getConstraints()) {
			int[] placesCounter = new int[N + 1];
			for (Variable var : constraint.vars) {
				for (Integer valInDomain : var.getDomain().getValues()) {
					placesCounter[valInDomain]++;
				}
			}

			for (int i = 1; i <= N; i++) {
				if (placesCounter[i] == 1) {
					for (Variable var : constraint.vars) {
						if (var.getDomain().contains(i)) {
							var.assignValue(i);
							break;
						}
					}
				}
			}
			if (!constraint.isConsistent()){
				return false;
			}
		}

		return true;
	}

	/**
	 * Optional TODO: Implement your own advanced Constraint Propagation
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private boolean getTournCC ( )
	{
		return false;
	}

	// =================================================================
	// Variable Selectors
	// =================================================================

	// Basic variable selector, returns first unassigned variable
	private Variable getfirstUnassignedVariable()
	{
		for ( Variable v : network.getVariables() )
			if ( ! v.isAssigned() )
				return v;

		// Everything is assigned
		return null;
	}

	/**
	 * Part 1 TODO: Implement the Minimum Remaining Value Heuristic
	 *
	 * Return: The unassigned variable with the smallest domain
	 */
	private Variable getMRV ( )
	{
		Variable mrvVar = null;
		int minLen = Integer.MAX_VALUE;
		for (Variable v : network.getVariables()) {
			if (!v.isAssigned() && v.size() < minLen) {
				mrvVar = v;
				minLen = v.size();
			}
		}
		return mrvVar;
	}

	/**
	 * Part 2 TODO: Implement the Degree Heuristic
	 *
	 * Return: The unassigned variable with the most unassigned neighbors
	 */
	private Variable getDegree ( )
	{
		Variable maxDegVar = null;
		int maxDegree = Integer.MIN_VALUE;
		
		for (Variable v : network.getVariables()) {
			if (!v.isAssigned()) {
				int degree = 0;
				for (Variable nei : network.getNeighborsOfVariable(v)) {
					if (!nei.isAssigned()) {
						degree++;
					}
				}
				if (degree > maxDegree) {
					maxDegree = degree;
					maxDegVar = v;
				}
			}
		}
		
		return  maxDegVar;
	}

	/**
	 * Part 2 TODO: Implement the Minimum Remaining Value Heuristic
	 *                with Degree Heuristic as a Tie Breaker
	 *
	 * Return: The unassigned variable with, first, the smallest domain
	 *         and, second, the most unassigned neighbors
	 */
	private Variable MRVwithTieBreaker ( )
	{
		Variable resVar = null;
		int resLen = Integer.MAX_VALUE;
		
		for (Variable v : network.getVariables()) {
			if (!v.isAssigned()) {
				if (v.size() < resLen) {
					resVar = v;
					resLen = v.size();
				} 
				else if (v.size() == resLen) {
					int resDegree = 0, curDegree = 0;
					for (Variable nei : network.getNeighborsOfVariable(resVar)) {
						if (!nei.isAssigned()) {
							resDegree++;
						}
					}
					for (Variable nei : network.getNeighborsOfVariable(v)) {
						if (!nei.isAssigned()) {
							curDegree++;
						}
					}
					if (curDegree > resDegree) {
						resVar = v;
					}
				}
			}
		}

		return resVar;
	}

	/**
	 * Optional TODO: Implement your own advanced Variable Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	private Variable getTournVar ( )
	{
		return null;
	}

	// =================================================================
	// Value Selectors
	// =================================================================

	// Default Value Ordering
	public List<Integer> getValuesInOrder ( Variable v )
	{
		List<Integer> values = v.getDomain().getValues();

		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}

	/**
	 * Part 1 TODO: Implement the Least Constraining Value Heuristic
	 *
	 * The Least constraining value is the one that will knock the least
	 * values out of it's neighbors domain.
	 *
	 * Return: A list of v's domain sorted by the LCV heuristic
	 *         The LCV is first and the MCV is last
	 */
	public List<Integer> getValuesLCVOrder ( Variable v )
	{
		List<Integer> domainVals = v.getDomain().getValues();
		Map<Integer, Integer> map = new HashMap<>();
		
		for (Integer val : domainVals) {
			map.put(val, 0);
		}
		
		for (Variable nei : network.getNeighborsOfVariable(v)) {
			for (Integer val : nei.getDomain().getValues()) {
				if (map.containsKey(val)) {
					map.put(val, map.get(val) + 1);
				}
			}
		}
		
		List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
			public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
				return e1.getValue().compareTo(e2.getValue());
			}
		});

		List<Integer> ans = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : list) {
			ans.add(entry.getKey());
		}

		return ans;
	}

	/**
	 * Optional TODO: Implement your own advanced Value Heuristic
	 *
	 * Completing the three tourn heuristic will automatically enter
	 * your program into a tournament.
	 */
	public List<Integer> getTournVal ( Variable v )
	{
		return null;
	}

	//==================================================================
	// Engine Functions
	//==================================================================

	public void solve ( )
	{
		if ( hasSolution )
			return;

		// Variable Selection
		Variable v = selectNextVariable();

		if ( v == null )
		{
			for ( Variable var : network.getVariables() )
			{
				// If all variables haven't been assigned
				if ( ! var.isAssigned() )
				{
					System.out.println( "Error" );
					return;
				}
			}

			// Success
			hasSolution = true;
			return;
		}

		// Attempt to assign a value
		for ( Integer i : getNextValues( v ) )
		{
			// Store place in trail and push variable's state on trail
			trail.placeTrailMarker();
			trail.push( v );

			// Assign the value
			v.assignValue( i );

			// Propagate constraints, check consistency, recurse
			if ( checkConsistency() )
				solve();

			// If this assignment succeeded, return
			if ( hasSolution )
				return;

			// Otherwise backtrack
			trail.undo();
		}
	}

	private boolean checkConsistency ( )
	{
		switch ( cChecks )
		{
			case "forwardChecking":
				return forwardChecking();

			case "norvigCheck":
				return norvigCheck();

			case "tournCC":
				return getTournCC();

			default:
				return assignmentsCheck();
		}
	}

	private Variable selectNextVariable ( )
	{
		switch ( varHeuristics )
		{
			case "MinimumRemainingValue":
				return getMRV();

			case "Degree":
				return getDegree();

			case "MRVwithTieBreaker":
				return MRVwithTieBreaker();

			case "tournVar":
				return getTournVar();

			default:
				return getfirstUnassignedVariable();
		}
	}

	public List<Integer> getNextValues ( Variable v )
	{
		switch ( valHeuristics )
		{
			case "LeastConstrainingValue":
				return getValuesLCVOrder( v );

			case "tournVal":
				return getTournVal( v );

			default:
				return getValuesInOrder( v );
		}
	}

	public boolean hasSolution ( )
	{
		return hasSolution;
	}

	public SudokuBoard getSolution ( )
	{
		return network.toSudokuBoard ( sudokuGrid.getP(), sudokuGrid.getQ() );
	}

	public ConstraintNetwork getNetwork ( )
	{
		return network;
	}
}

package org.mvel2.compiler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mvel2.integration.impl.SimpleValueResolver;
import org.mvel2.tests.BaseMvelTest;

/**
 * @author Viswa Ramamoorthy (viswaramamoorthy@yahoo.com)
 */
public class MvelCompileExpNullSafeTest extends BaseMvelTest
{
	@Test
	public void testMvelCompileNullSafeNullFirst() {
        String expression = "((parentGroup != null) && ($.?child.firstName in parentGroup.parentList if  $.?child.firstName != null).size() > 0)";
        ExpressionCompiler compiler = new ExpressionCompiler(expression, true);
        CompiledExpression compiledExpression = compiler.compile();
        
    	ParentGroup pGroup = new ParentGroup();
    	List<Parent> list = new ArrayList<Parent>();
    	Parent parent = new Parent();
    	parent.setChild(null);
    	list.add(parent);
    	pGroup.setParentList(list);

    	Boolean result = (Boolean)MVEL.executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == false;

    	Child child = new Child();
    	child.setFirstName("vlaa");

    	pGroup = new ParentGroup();
    	list = new ArrayList<Parent>();
    	parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	child = new Child();
    	child.setFirstName(null);
    	parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	pGroup.setParentList(list);

    	result = (Boolean)MVEL.executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == true;
	}

	@Test
	public void testMvelCompileNullSafeNullSecond() {
        String expression = "((parentGroup != null) && ($.?child.firstName in parentGroup.parentList if  $.?child.firstName != null).size() > 0)";
        ExpressionCompiler compiler = new ExpressionCompiler(expression, true);
        CompiledExpression compiledExpression = compiler.compile();
        
    	Child child = new Child();
    	child.setFirstName("vlaa");

    	ParentGroup pGroup = new ParentGroup();
    	List<Parent> list = new ArrayList<Parent>();
    	Parent parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	child = new Child();
    	child.setFirstName(null);
    	parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	pGroup.setParentList(list);

    	Boolean result = (Boolean)MVEL.executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == true;

    	pGroup = new ParentGroup();
    	list = new ArrayList<Parent>();
    	parent = new Parent();
    	parent.setChild(null);
    	list.add(parent);
    	pGroup.setParentList(list);

    	result = (Boolean)MVEL.executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == false;

	}
	
    @Test
    public void testCustomResolverFactory() {
        String expression = "($ in mylist if $.firstName=='Etnya').size() > 0";
        
        Child child = new Child();
        child.setFirstName("Etnya");
        
        List<Child> list = new ArrayList<Child>();
        list.add(child);
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("mylist", list);
        
        Boolean result = (Boolean)MVEL.eval(expression, new MyResolverFactory(map));
        assert result == true;
        
        list.clear();
        
        result = (Boolean)MVEL.eval(expression, new MyResolverFactory(map));
        assert result == false;
        
        child.setFirstName("Enjoy");
        list.add(child);
        
        result = (Boolean)MVEL.eval(expression, new MyResolverFactory(map));
        assert result == false;

    }

	/**
	 * Tests NullSafe with a segment which shadows a variable.
	 */
	@Test
	public void testNullSafeVariableOverlap() {
		String expression = "foo.?bar.baz"; // "baz" shadows the variable named "baz".

		final String expectedResult = "You got me!";

		Map<String, Object> foo = new HashMap<>();
		Map<String, Object> ctx = Collections.singletonMap("foo", foo);
		Map<String, Object> bar = Collections.singletonMap("baz", expectedResult);
		foo.put("bar", null); // start with null, as this will cause NullSafe to be added to the AST.

		// This variable overlaps the segment to be resolved next after NullSafe.
		Map<String, Object> vars = Collections.singletonMap("baz", "no, over here!");

		Serializable compiledExpr = MVEL.compileExpression(expression);
		Assert.assertNull(MVEL.executeExpression(compiledExpr, ctx, vars));

		foo.put("bar", bar);
		Assert.assertEquals(expectedResult, MVEL.executeExpression(compiledExpr, ctx, vars));
	}

	@Test
	public void testNullSafeWithVariableAsMapKey() {
		String expression = "foo.?bar[myvar]";

		final String expectedResult = "You got me!";

		Map<String, Object> foo = new HashMap<>();
		Map<String, Object> ctx = Collections.singletonMap("foo", foo);
		Map<String, Object> bar = Collections.singletonMap("baz", expectedResult);
		foo.put("bar", null); // start with null, as this will cause NullSafe to be added to the AST.

		Map<String, Object> vars = new HashMap<>();
		vars.put("myvar", "baz");

		Serializable compiledExpr = MVEL.compileExpression(expression);
		Assert.assertNull(MVEL.executeExpression(compiledExpr, ctx, vars));

		foo.put("bar", bar);
		Assert.assertEquals(expectedResult, MVEL.executeExpression(compiledExpr, ctx, vars));
	}

	@Test
	public void testNullSafeWithVariableAsMethodArg() {
		String expression = "foo.?bar.convert(myvar)";

		Map<String, Object> foo = new HashMap<>();
		Map<String, Object> ctx = Collections.singletonMap("foo", foo);
		foo.put("bar", null); // start with null, as this will cause NullSafe to be added to the AST.

		Map<String, Object> vars = new HashMap<>();
		vars.put("myvar", "hello");

		Serializable compiledExpr = MVEL.compileExpression(expression);
		Assert.assertNull(MVEL.executeExpression(compiledExpr, ctx, vars));

		foo.put("bar", new StringConverter());
		Assert.assertEquals("HELLO", MVEL.executeExpression(compiledExpr, ctx, vars));
	}

	public class StringConverter {
		public String convert(String value) {
			return value.toUpperCase();
		}
	}

	public class Child {
		private String firstName;
		private String lastName;
		
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
		
		public String getLastName() {
			return lastName;
		}
		
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}

    public class Parent {
    	private Child child;

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
    }

    public class ParentGroup {
    	private List<Parent> parentList;

		public List<Parent> getParentList() {
			return parentList;
		}

		public void setParentList(List<Parent> parentList) {
			this.parentList = parentList;
		}
    	
    }
    
    public class MyResolverFactory extends ImmutableDefaultFactory {
        private static final long serialVersionUID = 1L;
        private Map<String, Object> tempVariables;
        
        public MyResolverFactory(Map<String, Object> tempVariables) {
            this.tempVariables = tempVariables;
        }
        
        public boolean isResolveable(String name) {
            if (name instanceof String) {
                boolean result = tempVariables.containsKey(name);
                if (result) {
                    return result;
                }
                return super.isResolveable(name);
            }
            throw new IllegalArgumentException(
                  "MyResolverFactory can only resolve String variable names: " + name);
          }
        
         public VariableResolver getVariableResolver(String name) {
             if (tempVariables.containsKey(name)) {
                 return new SimpleValueResolver(tempVariables.get(name));
             }
             return super.getVariableResolver(name);
         }
    }
}

package com.github.springui5.model;

import static javax.script.ScriptContext.ENGINE_SCOPE;

import com.github.springui5.domain.Fruit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.DoubleVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Model for {@code home.view.js} view. Will be automatically serialized to Json via default {@linkplain
 * org.springframework.http.converter.HttpMessageConverter} configured by {@linkplain
 * org.springframework.web.servlet.config.annotation.EnableWebMvc} annotation on {@linkplain
 * com.github.springui5.conf.WebAppConfigurer} configuration class.
 *
 * @author gushakov
 * @author keilw
 */
public class HomeModel implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -443963300604827495L;

	private static final Logger logger = LoggerFactory.getLogger(HomeModel.class);

    private List<Fruit> listOfFruit;

    private String error;
    
    private final ScriptEngine engine;

    public List<Fruit> getListOfFruit() {
        return listOfFruit;
    }
    
    public Double getAverageQuantity() {
    	return averageQuantity(listOfFruit);
    }

    public void setListOfFruit(List<Fruit> listOfFruit) {
        this.listOfFruit = listOfFruit;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public HomeModel() {
        listOfFruit = new ArrayList<>(Arrays.asList(new Fruit("apple", 1), new Fruit("orange", 2)));
        engine = new RenjinScriptEngineFactory().getScriptEngine();
    }

    public HomeModel add(Fruit fruit) {
        // set id, it is 0 after deserializing from Json
        fruit.setId(Fruit.newId());
        listOfFruit.add(fruit);
        return this;
    }

    public HomeModel delete(final long id) {
    	// What does this do to DELETE?
        CollectionUtils.filter(listOfFruit, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((Fruit) object).getId() != id;
            }
        });
        return this;
    }

    public HomeModel update(final Fruit fruit) {
        // find the fruit with the same id
        Fruit oldFruit = (Fruit) CollectionUtils.find(listOfFruit, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((Fruit) object).getId() == fruit.getId();
            }
        });
        // update the fruit
        oldFruit.setName(fruit.getName());
        oldFruit.setQuantity(fruit.getQuantity());
        return this;
    }

    public HomeModel storeError(String error) {
        this.error = error;
        return this;
    }

    public HomeModel clearError() {
        this.error = null;
        return this;
    }

    public HomeModel show() {
        logger.debug(Arrays.toString(listOfFruit.toArray()));
        return this;
    }
    
    private double averageQuantity(List<Fruit> list) {
        double result = 0;
        final Bindings bindings = engine.getBindings(ENGINE_SCOPE);
        final StringBuilder sb = new StringBuilder();
        sb.append("x <- c(");
        int i=0;
        for (Fruit f : list) {
        	sb.append(f.getQuantity());
        	i++;
        	sb.append(i<list.size() ? ',' : ')');
        }
        try {
	      	engine.eval(sb.toString());
	      	engine.eval("a <- mean(x)");
	        DoubleVector a = (DoubleVector)bindings.get("a");
	      	if (a.isNumeric())    {
	      		result = a.getElementAsDouble(0);
	      		logger.debug(String.format("Average of %s = %s", sb.toString(), result));
	      	} else {
	      		logger.warn(String.format("%s does not result in a numeric value", sb.toString()));
	      	}
        } catch (ScriptException e) {
          throw new RuntimeException("Calculation failed");
        }
        return result;
      }
}

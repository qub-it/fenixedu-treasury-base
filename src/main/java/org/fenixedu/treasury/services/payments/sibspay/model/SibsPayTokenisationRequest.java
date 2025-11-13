package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TokenisationRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayTokenisationRequest   {
  @JsonProperty("tokeniseCard")
  private Boolean tokeniseCard = null;

  public SibsPayTokenisationRequest tokeniseCard(Boolean tokeniseCard) {
    this.tokeniseCard = tokeniseCard;
    return this;
  }

  /**
   * Get tokeniseCard
   * @return tokeniseCard
   **/
  
    public Boolean isTokeniseCard() {
    return tokeniseCard;
  }

  public void setTokeniseCard(Boolean tokeniseCard) {
    this.tokeniseCard = tokeniseCard;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SibsPayTokenisationRequest tokenisationRequest = (SibsPayTokenisationRequest) o;
    return Objects.equals(this.tokeniseCard, tokenisationRequest.tokeniseCard);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokeniseCard);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TokenisationRequest {\n");
    
    sb.append("    tokeniseCard: ").append(toIndentedString(tokeniseCard)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

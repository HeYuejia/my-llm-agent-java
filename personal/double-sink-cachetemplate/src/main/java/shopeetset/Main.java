package shopeetset;


import java.net.Inet4Address;
import java.util.*;

public class Main {
//    public int maxSubArray(ArrayList<Integer> nums) {
//        // write code here
//        if(nums.isEmpty() || nums == null) return 0;
//        int maxCur = nums.get(0);
//        int maxGlo = nums.get(0);
//
//        for(int i = 1 ; i < nums.size() ; i++){
//            maxCur = Math.max(nums.get(i) , maxCur+nums.get(i));
//            maxGlo = Math.max(maxCur,maxGlo);
//        }
//        return maxGlo;
//    }
//public String reverseMessage(String message) {
//    // write code here
//    if(message == null || message.isEmpty()) return "";
//
//    message = message.trim();
//
//    String[] words = message.split("\\s+");
//    StringBuilder reversed = new StringBuilder();
//    for(int i = words.length - 1 ; i >= 0; i--){
//        reversed.append(words[i]);
//        if(i > 0) reversed.append(" ");
//    }
//    return reversed.toString();
//}
    public ArrayList<Integer> maxNumber(ArrayList<Integer> nums1, ArrayList<Integer> nums2, int k) {
    // write code here
        int m = nums1.size();
        int n = nums2.size();

        ArrayList<Integer> res = new ArrayList<>();

        for(int i = Math.max(0, k - n); i <= Math.min(k,m) ; i++){
            List<Integer> max1 = selectKSub(nums1,i);
            List<Integer> max2 = selectKSub(nums2,k-i);
            ArrayList<Integer> merged = merge2list(max1,max2);
            res = selectMaxList(res,merged);
        }
        return  res;
    }

    private List<Integer> selectKSub(ArrayList<Integer> nums, int k){
        int drop = nums.size() - k;
        ArrayList<Integer> stack = new ArrayList<>();
        for(int num : nums){
            while(drop > 0 && !stack.isEmpty() && stack.get(stack.size() - 1) < num){
                stack.remove(stack.size() - 1);
                drop--;
            }
            stack.add(num);
        }
        return stack.subList(0,k);
    }

    private ArrayList<Integer> merge2list(List<Integer> nums1,List<Integer> nums2){
        ArrayList<Integer> merged = new ArrayList<>();
        int i = 0 ,j = 0;
        while(i < nums1.size() && j < nums2.size()){
            if(compareto(nums1,i,nums2,j) > 0)
                merged.add(nums1.get(i++));
            else
                merged.add(nums2.get(j++));
        }
        while(i < nums1.size()){
            merged.add(nums1.get(i++));
        }
        while(j < nums2.size()){
            merged.add(nums2.get(j++));
        }
        return  merged;
    }

    private int compareto(List<Integer> nums1,int i , List<Integer> nums2,int j){
        while(i < nums1.size() && j < nums2.size()){
            if(nums1.get(i) != nums2.get(j)) return nums1.get(i) - nums2.get(j);
            i++;
            j++;
        }
        return (i == nums1.size()) ? -1 : 1;
    }

    private ArrayList<Integer> selectMaxList(ArrayList<Integer> nums1, ArrayList<Integer> nums2){
        if(nums1.isEmpty()) return nums2;
        if(nums2.isEmpty()) return nums1;

        for(int i = 0 ; i < nums1.size() ; i++){
            if(nums1.get(i) != nums2.get(i)){
                return nums1.get(i) > nums2.get(i) ? nums1 : nums2;
            }
        }
        return  nums1;
    }

    public static void main(String[] args) {
        Main mymain = new Main();
        ArrayList<Integer> nums1 = new ArrayList<>();
        nums1.add(3);
        nums1.add(4);
        nums1.add(6);
        nums1.add(5);
        ArrayList<Integer> nums2 = new ArrayList<>();
        nums2.add(9);
        nums2.add(1);
        nums2.add(2);
        nums2.add(5);
        nums2.add(8);
        nums2.add(3);
        ArrayList<Integer> res = mymain.maxNumber(nums1,nums2,5);
        System.out.println(res);
    }
}

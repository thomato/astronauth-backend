import { createContext, useContext } from 'react';
import { en, type Copy } from './en';

/** English only for now; a provider higher up can supply another language later. */
export const CopyContext = createContext<Copy>(en);

export function useCopy(): Copy {
  return useContext(CopyContext);
}
